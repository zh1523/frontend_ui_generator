package com.example.uigen.service;

import com.example.uigen.model.dto.LlmStreamResult;
import com.example.uigen.model.dto.SafetyScanResult;
import com.example.uigen.model.dto.SfcSections;
import com.example.uigen.model.entity.ComponentVersion;
import com.example.uigen.model.entity.GenerationTask;
import com.example.uigen.model.entity.LlmCallLog;
import com.example.uigen.model.enums.SafetyLevel;
import com.example.uigen.model.enums.TaskStatus;
import com.example.uigen.repository.ComponentVersionRepository;
import com.example.uigen.repository.LlmCallLogRepository;
import com.example.uigen.service.llm.QwenLlmClient;
import com.example.uigen.service.parser.SfcCodeExtractor;
import com.example.uigen.service.safety.CodeSafetyScanner;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class GenerationStreamService {

    private final GenerationService generationService;
    private final QwenLlmClient qwenLlmClient;
    private final SfcCodeExtractor sfcCodeExtractor;
    private final CodeSafetyScanner codeSafetyScanner;
    private final ComponentVersionRepository componentVersionRepository;
    private final LlmCallLogRepository llmCallLogRepository;
    private final CostControlService costControlService;
    private final GenerationCacheService generationCacheService;

    private final ConcurrentHashMap<Long, AtomicBoolean> runningMap = new ConcurrentHashMap<>();

    public GenerationStreamService(GenerationService generationService,
                                   QwenLlmClient qwenLlmClient,
                                   SfcCodeExtractor sfcCodeExtractor,
                                   CodeSafetyScanner codeSafetyScanner,
                                   ComponentVersionRepository componentVersionRepository,
                                   LlmCallLogRepository llmCallLogRepository,
                                   CostControlService costControlService,
                                   GenerationCacheService generationCacheService) {
        this.generationService = generationService;
        this.qwenLlmClient = qwenLlmClient;
        this.sfcCodeExtractor = sfcCodeExtractor;
        this.codeSafetyScanner = codeSafetyScanner;
        this.componentVersionRepository = componentVersionRepository;
        this.llmCallLogRepository = llmCallLogRepository;
        this.costControlService = costControlService;
        this.generationCacheService = generationCacheService;
    }

    @Async("generationExecutor")
    public void generateAndStream(GenerationTask task, SseEmitter emitter) {
        Long taskId = task.getId();
        if (task.getStatus() == TaskStatus.SUCCEEDED) {
            componentVersionRepository.findTopByTaskIdOrderByVersionNoDesc(taskId).ifPresent(version ->
                    emit(emitter, "final_code", Map.of(
                            "taskId", taskId,
                            "versionId", version.getId(),
                            "versionNo", version.getVersionNo(),
                            "code", version.getVueCode(),
                            "safetyLevel", version.getSafetyLevel().name()
                    )));
            emit(emitter, "done", Map.of("taskId", taskId, "status", TaskStatus.SUCCEEDED.name()));
            emitter.complete();
            return;
        }
        AtomicBoolean lock = runningMap.computeIfAbsent(taskId, ignored -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            emitError(emitter, "Task is already generating");
            emitter.complete();
            return;
        }
        try {
            emit(emitter, "started", Map.of("taskId", taskId));
            generationService.markTaskGenerating(task);
            Long ownerUserId = getOwnerUserId(task);
            if (ownerUserId != null) {
                costControlService.checkQuotaOrThrow(ownerUserId);
            }

            GenerationCacheService.CachedGeneration cached = generationCacheService.get(task);
            if (cached != null) {
                ComponentVersion version = saveVersion(task, cached.sections(), cached.safetyScanResult());
                generationService.markTaskSucceeded(task);
                emit(emitter, "final_code", Map.of(
                        "taskId", taskId,
                        "versionId", version.getId(),
                        "versionNo", version.getVersionNo(),
                        "code", version.getVueCode(),
                        "safetyLevel", version.getSafetyLevel().name(),
                        "cacheHit", true
                ));
                emit(emitter, "done", Map.of("taskId", taskId, "status", TaskStatus.SUCCEEDED.name()));
                emitter.complete();
                return;
            }

            StringBuilder partialBuilder = new StringBuilder();
            LlmStreamResult result = qwenLlmClient.streamGenerate(task, token -> {
                partialBuilder.append(token);
                emit(emitter, "token", Map.of("taskId", taskId, "token", token));
            });
            SfcSections sections = sfcCodeExtractor.extract(result.content());
            SafetyScanResult safetyScanResult = codeSafetyScanner.scan(sections.vueCode());

            if (safetyScanResult.level() == SafetyLevel.BLOCKED) {
                generationService.markTaskFailed(task, safetyScanResult.reason());
                saveCallLog(task, result);
                emit(emitter, "error", Map.of("taskId", taskId, "message", safetyScanResult.reason()));
                emit(emitter, "done", Map.of("taskId", taskId, "status", TaskStatus.FAILED.name()));
                emitter.complete();
                return;
            }

            ComponentVersion version = saveVersion(task, sections, safetyScanResult);
            generationService.markTaskSucceeded(task);
            saveCallLog(task, result);
            generationCacheService.put(task, sections, safetyScanResult);

            emit(emitter, "partial_code", Map.of("taskId", taskId, "text", partialBuilder.toString()));
            emit(emitter, "final_code", Map.of(
                    "taskId", taskId,
                    "versionId", version.getId(),
                    "versionNo", version.getVersionNo(),
                    "code", version.getVueCode(),
                    "safetyLevel", version.getSafetyLevel().name()
            ));
            emit(emitter, "done", Map.of("taskId", taskId, "status", TaskStatus.SUCCEEDED.name()));
            emitter.complete();
        } catch (Exception ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Generation failed"
                    : ex.getMessage();
            generationService.markTaskFailed(task, message);
            emitError(emitter, message);
            emitter.complete();
        } finally {
            lock.set(false);
            runningMap.remove(taskId, lock);
        }
    }

    private void emit(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException ignored) {
        }
    }

    private void emitError(SseEmitter emitter, String message) {
        String msg = (message == null || message.isBlank()) ? "Unexpected error" : message;
        emit(emitter, "error", Map.of("message", msg));
    }

    @Transactional
    protected ComponentVersion saveVersion(GenerationTask task, SfcSections sections, SafetyScanResult safetyScanResult) {
        int max = componentVersionRepository.findMaxVersionNo(task.getId());
        ComponentVersion version = new ComponentVersion();
        version.setTask(task);
        version.setVersionNo(max + 1);
        version.setVueCode(sections.vueCode());
        version.setTemplateCode(sections.templateCode());
        version.setScriptCode(sections.scriptCode());
        version.setStyleCode(sections.styleCode());
        version.setSafetyLevel(safetyScanResult.level());
        version.setSafetyReason(safetyScanResult.reason());
        version.setCompileOk(!sections.templateCode().isBlank() && !sections.scriptCode().isBlank());
        version.setDownloadCount(0);
        version.setCreatedAt(Instant.now());
        return componentVersionRepository.save(version);
    }

    @Transactional
    protected void saveCallLog(GenerationTask task, LlmStreamResult result) {
        LlmCallLog callLog = new LlmCallLog();
        callLog.setTask(task);
        callLog.setProvider("dashscope");
        callLog.setModel(task.getModel());
        int requestTokens = result.requestTokens() == null ? 0 : result.requestTokens();
        int responseTokens = result.responseTokens() == null ? 0 : result.responseTokens();
        int totalTokens = requestTokens + responseTokens;
        double estimatedCost = costControlService.estimateCostUsd(requestTokens, responseTokens);
        callLog.setRequestTokens(requestTokens);
        callLog.setResponseTokens(responseTokens);
        callLog.setTotalTokens(totalTokens);
        callLog.setEstimatedCostUsd(estimatedCost);
        callLog.setLatencyMs(result.latencyMs());
        callLog.setFinishReason(result.finishReason());
        callLog.setCreatedAt(Instant.now());
        llmCallLogRepository.save(callLog);
        Long ownerUserId = getOwnerUserId(task);
        if (ownerUserId != null) {
            costControlService.recordUsage(ownerUserId, totalTokens, estimatedCost);
        }
    }

    private Long getOwnerUserId(GenerationTask task) {
        if (task == null || task.getProject() == null || task.getProject().getOwnerUser() == null) {
            return null;
        }
        return task.getProject().getOwnerUser().getId();
    }
}

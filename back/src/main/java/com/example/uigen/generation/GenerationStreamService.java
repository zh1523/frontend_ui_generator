package com.example.uigen.generation;

import com.example.uigen.llm.LlmCallLog;
import com.example.uigen.llm.LlmCallLogRepository;
import com.example.uigen.llm.LlmStreamResult;
import com.example.uigen.llm.QwenLlmClient;
import com.example.uigen.safety.CodeSafetyScanner;
import com.example.uigen.safety.SafetyLevel;
import com.example.uigen.safety.SafetyScanResult;
import com.example.uigen.version.ComponentVersion;
import com.example.uigen.version.ComponentVersionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GenerationStreamService {

    private final GenerationService generationService;
    private final QwenLlmClient qwenLlmClient;
    private final SfcCodeExtractor sfcCodeExtractor;
    private final CodeSafetyScanner codeSafetyScanner;
    private final ComponentVersionRepository componentVersionRepository;
    private final LlmCallLogRepository llmCallLogRepository;

    private final ConcurrentHashMap<Long, AtomicBoolean> runningMap = new ConcurrentHashMap<>();

    public GenerationStreamService(GenerationService generationService,
                                   QwenLlmClient qwenLlmClient,
                                   SfcCodeExtractor sfcCodeExtractor,
                                   CodeSafetyScanner codeSafetyScanner,
                                   ComponentVersionRepository componentVersionRepository,
                                   LlmCallLogRepository llmCallLogRepository) {
        this.generationService = generationService;
        this.qwenLlmClient = qwenLlmClient;
        this.sfcCodeExtractor = sfcCodeExtractor;
        this.codeSafetyScanner = codeSafetyScanner;
        this.componentVersionRepository = componentVersionRepository;
        this.llmCallLogRepository = llmCallLogRepository;
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
        callLog.setRequestTokens(result.requestTokens() == null ? 0 : result.requestTokens());
        callLog.setResponseTokens(result.responseTokens() == null ? 0 : result.responseTokens());
        callLog.setLatencyMs(result.latencyMs());
        callLog.setFinishReason(result.finishReason());
        callLog.setCreatedAt(Instant.now());
        llmCallLogRepository.save(callLog);
    }
}

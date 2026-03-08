package com.example.uigen.generation;

import com.example.uigen.common.ApiException;
import com.example.uigen.common.PagedResponse;
import com.example.uigen.config.AppProperties;
import com.example.uigen.version.ComponentVersion;
import com.example.uigen.version.ComponentVersionRepository;
import com.example.uigen.version.ComponentVersionSummaryResponse;
import com.example.uigen.workspace.Workspace;
import com.example.uigen.workspace.WorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GenerationService {

    private final GenerationTaskRepository generationTaskRepository;
    private final ComponentVersionRepository componentVersionRepository;
    private final WorkspaceService workspaceService;
    private final AppProperties appProperties;

    public GenerationService(GenerationTaskRepository generationTaskRepository,
                             ComponentVersionRepository componentVersionRepository,
                             WorkspaceService workspaceService,
                             AppProperties appProperties) {
        this.generationTaskRepository = generationTaskRepository;
        this.componentVersionRepository = componentVersionRepository;
        this.workspaceService = workspaceService;
        this.appProperties = appProperties;
    }

    @Transactional
    public GenerationTaskCreateResponse createTask(String workspaceKey, GenerateRequest request) {
        Workspace workspace = workspaceService.requireActiveWorkspace(workspaceKey);
        validateRequest(request);

        GenerationTask task = new GenerationTask();
        task.setWorkspace(workspace);
        task.setPrompt(request.prompt().trim());
        task.setComponentName(request.componentName().trim());
        task.setConstraintsJson(defaultConstraints(request.constraints()));
        task.setStatus(TaskStatus.PENDING);
        task.setModel(appProperties.llm().model());
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        generationTaskRepository.save(task);
        return new GenerationTaskCreateResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public GenerationTask requireTaskInWorkspace(Long taskId, String workspaceKey) {
        Workspace workspace = workspaceService.requireActiveWorkspace(workspaceKey);
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(404, "Task not found"));
        if (!task.getWorkspace().getId().equals(workspace.getId())) {
            throw new ApiException(403, "Task is not in current workspace");
        }
        return task;
    }

    @Transactional
    public GenerationTaskDetailResponse getTaskDetail(Long taskId, String workspaceKey) {
        GenerationTask task = requireTaskInWorkspace(taskId, workspaceKey);
        Optional<ComponentVersionSummaryResponse> latest = componentVersionRepository
                .findTopByTaskIdOrderByVersionNoDesc(taskId)
                .map(this::toVersionSummary);
        return new GenerationTaskDetailResponse(
                task.getId(),
                task.getPrompt(),
                task.getComponentName(),
                task.getConstraintsJson(),
                task.getModel(),
                task.getStatus(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getStartedAt(),
                task.getFinishedAt(),
                latest.orElse(null)
        );
    }

    @Transactional
    public PagedResponse<GenerationTaskSummaryResponse> listWorkspaceTasks(String workspaceKey, int page, int size) {
        Workspace workspace = workspaceService.requireActiveWorkspace(workspaceKey);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GenerationTask> taskPage = generationTaskRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId(), pageRequest);
        List<GenerationTaskSummaryResponse> items = taskPage.getContent().stream()
                .map(this::toTaskSummary)
                .toList();
        return new PagedResponse<>(items, taskPage.getTotalElements(), page, size);
    }

    @Transactional
    public GenerationTaskCreateResponse regenerateTask(Long taskId, String workspaceKey) {
        GenerationTask task = requireTaskInWorkspace(taskId, workspaceKey);
        if (task.getStatus() == TaskStatus.GENERATING) {
            throw new ApiException(409, "Task is already generating");
        }
        task.setStatus(TaskStatus.PENDING);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setUpdatedAt(Instant.now());
        return new GenerationTaskCreateResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public void markTaskGenerating(GenerationTask task) {
        task.setStatus(TaskStatus.GENERATING);
        task.setErrorMessage(null);
        task.setStartedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        generationTaskRepository.save(task);
    }

    @Transactional
    public void markTaskSucceeded(GenerationTask task) {
        task.setStatus(TaskStatus.SUCCEEDED);
        task.setFinishedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        generationTaskRepository.save(task);
    }

    @Transactional
    public void markTaskFailed(GenerationTask task, String errorMessage) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        generationTaskRepository.save(task);
    }

    private GenerationTaskSummaryResponse toTaskSummary(GenerationTask task) {
        Optional<ComponentVersion> latestVersion = componentVersionRepository.findTopByTaskIdOrderByVersionNoDesc(task.getId());
        return latestVersion
                .map(version -> new GenerationTaskSummaryResponse(
                        task.getId(),
                        task.getPrompt(),
                        task.getComponentName(),
                        task.getStatus(),
                        task.getCreatedAt(),
                        task.getUpdatedAt(),
                        version.getId(),
                        version.getVersionNo(),
                        version.getSafetyLevel()
                ))
                .orElseGet(() -> new GenerationTaskSummaryResponse(
                        task.getId(),
                        task.getPrompt(),
                        task.getComponentName(),
                        task.getStatus(),
                        task.getCreatedAt(),
                        task.getUpdatedAt(),
                        null,
                        null,
                        null
                ));
    }

    private ComponentVersionSummaryResponse toVersionSummary(ComponentVersion version) {
        return new ComponentVersionSummaryResponse(
                version.getId(),
                version.getTask().getId(),
                version.getVersionNo(),
                version.getSafetyLevel(),
                version.getSafetyReason(),
                version.isCompileOk(),
                version.getCreatedAt()
        );
    }

    private void validateRequest(GenerateRequest request) {
        if (request.prompt().length() > appProperties.generation().maxPromptLength()) {
            throw new ApiException(400, "Prompt exceeds max length");
        }
        if (request.componentName().length() > appProperties.generation().maxComponentNameLength()) {
            throw new ApiException(400, "Component name exceeds max length");
        }
    }

    private String defaultConstraints(String constraints) {
        if (constraints == null || constraints.isBlank()) {
            return "{}";
        }
        return constraints.trim();
    }
}

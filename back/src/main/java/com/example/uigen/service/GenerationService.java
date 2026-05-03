package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.common.AuthContextHolder;
import com.example.uigen.common.PagedResponse;
import com.example.uigen.config.AppProperties;
import com.example.uigen.model.dto.ComponentVersionSummaryResponse;
import com.example.uigen.model.dto.GenerateRequest;
import com.example.uigen.model.dto.GenerationTaskCreateResponse;
import com.example.uigen.model.dto.GenerationTaskDetailResponse;
import com.example.uigen.model.dto.GenerationTaskSummaryResponse;
import com.example.uigen.model.entity.ComponentVersion;
import com.example.uigen.model.entity.GenerationTask;
import com.example.uigen.model.entity.ProjectSpace;
import com.example.uigen.model.entity.Workspace;
import com.example.uigen.model.enums.TaskStatus;
import com.example.uigen.repository.ComponentVersionRepository;
import com.example.uigen.repository.GenerationTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationService {

    private final GenerationTaskRepository generationTaskRepository;
    private final ComponentVersionRepository componentVersionRepository;
    private final WorkspaceService workspaceService;
    private final ProjectService projectService;
    private final AppProperties appProperties;

    public GenerationService(GenerationTaskRepository generationTaskRepository,
                             ComponentVersionRepository componentVersionRepository,
                             WorkspaceService workspaceService,
                             ProjectService projectService,
                             AppProperties appProperties) {
        this.generationTaskRepository = generationTaskRepository;
        this.componentVersionRepository = componentVersionRepository;
        this.workspaceService = workspaceService;
        this.projectService = projectService;
        this.appProperties = appProperties;
    }

    @Transactional
    public GenerationTaskCreateResponse createTask(String workspaceKey, Long projectId, GenerateRequest request) {
        Long userId = requireUserId();
        ProjectSpace project = projectService.requireMyProject(projectId);
        Workspace workspace = workspaceService.requireWorkspaceForProject(workspaceKey, project, userId);
        validateRequest(request);

        GenerationTask task = new GenerationTask();
        task.setWorkspace(workspace);
        task.setProject(project);
        task.setPrompt(request.prompt().trim());
        task.setComponentName(request.componentName().trim());
        task.setConstraintsJson(defaultConstraints(request.constraints()));
        task.setIncludeDemoData(resolveIncludeDemoData(request.includeDemoData()));
        task.setStatus(TaskStatus.PENDING);
        task.setModel(appProperties.llm().model());
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        generationTaskRepository.save(task);
        return new GenerationTaskCreateResponse(task.getId(), task.getStatus());
    }

    @Transactional
    public GenerationTask requireTaskInWorkspace(Long taskId, String workspaceKey, Long projectId) {
        Long userId = requireUserId();
        ProjectSpace project = projectService.requireMyProject(projectId);
        Workspace workspace = workspaceService.requireWorkspaceForProject(workspaceKey, project, userId);
        GenerationTask task = generationTaskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(404, "Task not found"));
        if (task.getProject() == null || !task.getProject().getId().equals(project.getId())) {
            throw new ApiException(403, "Task is not in current project");
        }
        if (!task.getWorkspace().getId().equals(workspace.getId())) {
            throw new ApiException(403, "Task is not in current workspace");
        }
        return task;
    }

    @Transactional
    public GenerationTaskDetailResponse getTaskDetail(Long taskId, String workspaceKey, Long projectId) {
        GenerationTask task = requireTaskInWorkspace(taskId, workspaceKey, projectId);
        Optional<ComponentVersionSummaryResponse> latest = componentVersionRepository
                .findTopByTaskIdOrderByVersionNoDesc(taskId)
                .map(this::toVersionSummary);
        return new GenerationTaskDetailResponse(
                task.getId(),
                task.getProject() == null ? null : task.getProject().getId(),
                task.getPrompt(),
                task.getComponentName(),
                task.getConstraintsJson(),
                task.isIncludeDemoData(),
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
    public PagedResponse<GenerationTaskSummaryResponse> listProjectTasks(Long projectId, int page, int size) {
        ProjectSpace project = projectService.requireMyProject(projectId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GenerationTask> taskPage = generationTaskRepository.findByProjectIdOrderByCreatedAtDesc(project.getId(), pageRequest);
        List<GenerationTaskSummaryResponse> items = taskPage.getContent().stream()
                .map(this::toTaskSummary)
                .toList();
        return new PagedResponse<>(items, taskPage.getTotalElements(), page, size);
    }

    @Transactional
    public GenerationTaskCreateResponse regenerateTask(Long taskId, String workspaceKey, Long projectId) {
        GenerationTask task = requireTaskInWorkspace(taskId, workspaceKey, projectId);
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
                        task.getProject() == null ? null : task.getProject().getId(),
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
                        task.getProject() == null ? null : task.getProject().getId(),
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

    private boolean resolveIncludeDemoData(Boolean includeDemoData) {
        return includeDemoData == null || includeDemoData;
    }

    private Long requireUserId() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            throw new ApiException(401, "Unauthorized");
        }
        return userId;
    }
}

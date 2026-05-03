package com.example.uigen.controller;

import com.example.uigen.common.HttpHeadersConst;
import com.example.uigen.common.PagedResponse;
import com.example.uigen.model.dto.GenerateRequest;
import com.example.uigen.model.dto.GenerationTaskCreateResponse;
import com.example.uigen.model.dto.GenerationTaskDetailResponse;
import com.example.uigen.model.dto.GenerationTaskSummaryResponse;
import com.example.uigen.model.entity.GenerationTask;
import com.example.uigen.service.GenerationRateLimiter;
import com.example.uigen.service.GenerationService;
import com.example.uigen.service.GenerationStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Validated
@RequestMapping("/api/v1")
public class GenerationController {

    private final GenerationService generationService;
    private final GenerationStreamService generationStreamService;
    private final GenerationRateLimiter generationRateLimiter;

    public GenerationController(GenerationService generationService,
                                GenerationStreamService generationStreamService,
                                GenerationRateLimiter generationRateLimiter) {
        this.generationService = generationService;
        this.generationStreamService = generationStreamService;
        this.generationRateLimiter = generationRateLimiter;
    }

    @PostMapping("/generations")
    public GenerationTaskCreateResponse createTask(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @Valid @RequestBody GenerateRequest request
    ) {
        generationRateLimiter.checkOrThrow("project:" + projectId);
        return generationService.createTask(workspaceKey, projectId, request);
    }

    @GetMapping(value = "/generations/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long taskId
    ) {
        GenerationTask task = generationService.requireTaskInWorkspace(taskId, workspaceKey, projectId);
        SseEmitter emitter = new SseEmitter(0L);
        generationStreamService.generateAndStream(task, emitter);
        return emitter;
    }

    @GetMapping("/generations/{taskId}")
    public GenerationTaskDetailResponse getTaskDetail(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long taskId
    ) {
        return generationService.getTaskDetail(taskId, workspaceKey, projectId);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PagedResponse<GenerationTaskSummaryResponse> listWorkspaceTasks(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        return generationService.listProjectTasks(projectId, safePage, safeSize);
    }

    @PostMapping("/generations/{taskId}/regenerate")
    public GenerationTaskCreateResponse regenerateTask(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestHeader(HttpHeadersConst.PROJECT_ID) Long projectId,
            @PathVariable Long taskId
    ) {
        generationRateLimiter.checkOrThrow("project:" + projectId);
        return generationService.regenerateTask(taskId, workspaceKey, projectId);
    }
}

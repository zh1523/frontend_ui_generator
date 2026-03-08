package com.example.uigen.generation;

import com.example.uigen.common.HttpHeadersConst;
import com.example.uigen.common.PagedResponse;
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
            @Valid @RequestBody GenerateRequest request
    ) {
        generationRateLimiter.checkOrThrow(workspaceKey);
        return generationService.createTask(workspaceKey, request);
    }

    @GetMapping(value = "/generations/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @PathVariable Long taskId
    ) {
        GenerationTask task = generationService.requireTaskInWorkspace(taskId, workspaceKey);
        SseEmitter emitter = new SseEmitter(0L);
        generationStreamService.generateAndStream(task, emitter);
        return emitter;
    }

    @GetMapping("/generations/{taskId}")
    public GenerationTaskDetailResponse getTaskDetail(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @PathVariable Long taskId
    ) {
        return generationService.getTaskDetail(taskId, workspaceKey);
    }

    @GetMapping("/workspaces/me/tasks")
    public PagedResponse<GenerationTaskSummaryResponse> listWorkspaceTasks(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        return generationService.listWorkspaceTasks(workspaceKey, safePage, safeSize);
    }

    @PostMapping("/generations/{taskId}/regenerate")
    public GenerationTaskCreateResponse regenerateTask(
            @RequestHeader(HttpHeadersConst.WORKSPACE_KEY) String workspaceKey,
            @PathVariable Long taskId
    ) {
        generationRateLimiter.checkOrThrow(workspaceKey);
        return generationService.regenerateTask(taskId, workspaceKey);
    }
}

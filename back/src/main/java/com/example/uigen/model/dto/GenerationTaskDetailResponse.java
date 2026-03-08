package com.example.uigen.model.dto;

import com.example.uigen.model.enums.TaskStatus;
import java.time.Instant;

public record GenerationTaskDetailResponse(
        Long id,
        Long projectId,
        String prompt,
        String componentName,
        String constraints,
        boolean includeDemoData,
        String model,
        TaskStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        ComponentVersionSummaryResponse latestVersion
) {
}

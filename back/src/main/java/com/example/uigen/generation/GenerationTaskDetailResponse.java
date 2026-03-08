package com.example.uigen.generation;

import com.example.uigen.version.ComponentVersionSummaryResponse;

import java.time.Instant;

public record GenerationTaskDetailResponse(
        Long id,
        String prompt,
        String componentName,
        String constraints,
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

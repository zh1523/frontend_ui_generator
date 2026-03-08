package com.example.uigen.generation;

import com.example.uigen.safety.SafetyLevel;

import java.time.Instant;

public record GenerationTaskSummaryResponse(
        Long id,
        String prompt,
        String componentName,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long latestVersionId,
        Integer latestVersionNo,
        SafetyLevel latestSafetyLevel
) {
}

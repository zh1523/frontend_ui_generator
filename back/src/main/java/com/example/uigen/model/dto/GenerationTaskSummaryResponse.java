package com.example.uigen.model.dto;

import com.example.uigen.model.enums.SafetyLevel;
import com.example.uigen.model.enums.TaskStatus;
import java.time.Instant;

public record GenerationTaskSummaryResponse(
        Long id,
        Long projectId,
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

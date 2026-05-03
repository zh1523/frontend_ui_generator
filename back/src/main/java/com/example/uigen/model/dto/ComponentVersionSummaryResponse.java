package com.example.uigen.model.dto;

import com.example.uigen.model.enums.SafetyLevel;
import java.time.Instant;

public record ComponentVersionSummaryResponse(
        Long id,
        Long taskId,
        Integer versionNo,
        SafetyLevel safetyLevel,
        String safetyReason,
        boolean compileOk,
        Instant createdAt
) {
}

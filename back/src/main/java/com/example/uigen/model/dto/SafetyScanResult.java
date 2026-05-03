package com.example.uigen.model.dto;

import com.example.uigen.model.enums.SafetyLevel;

public record SafetyScanResult(
        SafetyLevel level,
        String reason
) {
}

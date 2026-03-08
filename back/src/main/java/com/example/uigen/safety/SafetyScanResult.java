package com.example.uigen.safety;

public record SafetyScanResult(
        SafetyLevel level,
        String reason
) {
}

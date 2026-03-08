package com.example.uigen.model.dto;

public record LlmStreamResult(
        String content,
        Integer requestTokens,
        Integer responseTokens,
        String finishReason,
        long latencyMs
) {
}

package com.example.uigen.llm;

public record LlmStreamResult(
        String content,
        Integer requestTokens,
        Integer responseTokens,
        String finishReason,
        long latencyMs
) {
}

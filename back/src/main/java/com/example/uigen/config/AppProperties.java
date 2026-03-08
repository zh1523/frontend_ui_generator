package com.example.uigen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Llm llm,
        Generation generation
) {
    public record Cors(
            String allowedOrigins
    ) {
    }

    public record Llm(
            String baseUrl,
            String apiKey,
            String model,
            Integer timeoutMs
    ) {
    }

    public record Generation(
            Integer maxPromptLength,
            Integer maxComponentNameLength
    ) {
    }
}

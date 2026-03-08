package com.example.uigen.llm;

import com.example.uigen.common.ApiException;
import com.example.uigen.config.AppProperties;
import com.example.uigen.generation.GenerationTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class QwenLlmClient {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public QwenLlmClient(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public LlmStreamResult streamGenerate(GenerationTask task, Consumer<String> onToken) {
        if (properties.llm().apiKey() == null || properties.llm().apiKey().isBlank()) {
            throw new ApiException(500, "LLM API key is not configured");
        }
        try {
            Map<String, Object> payload = buildRequestPayload(task);
            String body = objectMapper.writeValueAsString(payload);
            String endpoint = properties.llm().baseUrl().replaceAll("/$", "") + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(properties.llm().timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.llm().apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            long start = System.currentTimeMillis();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new ApiException(502, "LLM provider error: " + errorBody);
            }
            StreamReadResult streamReadResult = readStream(response.body(), onToken);
            long latency = System.currentTimeMillis() - start;
            int completionTokens = streamReadResult.responseTokens() > 0
                    ? streamReadResult.responseTokens()
                    : estimateTokens(streamReadResult.content().length());
            return new LlmStreamResult(
                    streamReadResult.content(),
                    streamReadResult.requestTokens(),
                    completionTokens,
                    streamReadResult.finishReason(),
                    latency
            );
        } catch (IOException e) {
            throw new ApiException(502, "LLM stream IO error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "LLM stream interrupted");
        }
    }

    private Map<String, Object> buildRequestPayload(GenerationTask task) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", task.getModel());
        payload.put("stream", true);
        payload.put("temperature", 0.2);
        payload.put("messages", new Object[]{
                Map.of("role", "system", "content", """
                        You are a Vue 3 component generator.
                        Always return exactly one valid Vue Single File Component.
                        Requirements:
                        1) Use <script setup> with JavaScript, not TypeScript.
                        2) Keep output self-contained and do not import external URLs.
                        3) Include <template>, <script setup>, and <style scoped>.
                        4) Do not include explanation text outside code.
                        """),
                Map.of("role", "user", "content", buildUserPrompt(task))
        });
        return payload;
    }

    private String buildUserPrompt(GenerationTask task) {
        String constraints = task.getConstraintsJson();
        if (constraints == null || constraints.isBlank()) {
            constraints = "{}";
        }
        return """
                Generate a Vue 3 SFC component.
                Component name: %s
                User requirement: %s
                Constraints(json): %s
                """.formatted(task.getComponentName(), task.getPrompt(), constraints);
    }

    private StreamReadResult readStream(InputStream inputStream, Consumer<String> onToken) throws IOException {
        StringBuilder content = new StringBuilder();
        int requestTokens = 0;
        int responseTokens = 0;
        String finishReason = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring(5).trim();
                if (data.isBlank()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    JsonNode choice = choices.get(0);
                    String token = choice.path("delta").path("content").asText("");
                    if (!token.isEmpty()) {
                        content.append(token);
                        onToken.accept(token);
                    }
                    String end = choice.path("finish_reason").asText("");
                    if (!end.isBlank()) {
                        finishReason = end;
                    }
                }
                JsonNode usage = root.path("usage");
                if (!usage.isMissingNode()) {
                    requestTokens = usage.path("prompt_tokens").asInt(requestTokens);
                    responseTokens = usage.path("completion_tokens").asInt(responseTokens);
                }
            }
        }
        return new StreamReadResult(content.toString(), requestTokens, responseTokens, finishReason);
    }

    private int estimateTokens(int length) {
        return Math.max(1, length / 4);
    }

    private record StreamReadResult(
            String content,
            int requestTokens,
            int responseTokens,
            String finishReason
    ) {
    }
}

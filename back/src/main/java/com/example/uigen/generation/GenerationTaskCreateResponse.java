package com.example.uigen.generation;

public record GenerationTaskCreateResponse(
        Long taskId,
        TaskStatus status
) {
}

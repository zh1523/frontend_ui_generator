package com.example.uigen.model.dto;

import com.example.uigen.model.enums.TaskStatus;

public record GenerationTaskCreateResponse(
        Long taskId,
        TaskStatus status
) {
}

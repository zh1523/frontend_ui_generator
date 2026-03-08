package com.example.uigen.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateRequest(
        @NotBlank
        @Size(max = 2000)
        String prompt,
        @NotBlank
        @Size(max = 80)
        String componentName,
        String constraints
) {
}

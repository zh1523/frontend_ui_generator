package com.example.uigen.model.dto;

import com.example.uigen.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull
        UserRole role
) {
}

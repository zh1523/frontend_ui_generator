package com.example.uigen.model.dto;

import com.example.uigen.model.enums.UserRole;

public record UserProfileResponse(
        Long userId,
        String username,
        UserRole role
) {
}

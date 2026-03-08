package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.common.AuthContextHolder;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.enums.Permission;
import com.example.uigen.model.enums.UserRole;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;

@Service
public class AuthorizationService {

    private static final Map<UserRole, EnumSet<Permission>> ROLE_PERMISSION_MAP = Map.of(
            UserRole.USER, EnumSet.noneOf(Permission.class),
            UserRole.ADMIN, EnumSet.of(Permission.USER_MANAGE, Permission.AUDIT_READ)
    );

    public AppUser requireCurrentUser() {
        AppUser user = AuthContextHolder.getUser();
        if (user == null) {
            throw new ApiException(401, "Unauthorized");
        }
        return user;
    }

    public void requirePermission(Permission permission) {
        AppUser user = requireCurrentUser();
        EnumSet<Permission> permissions = ROLE_PERMISSION_MAP.getOrDefault(user.getRole(), EnumSet.noneOf(Permission.class));
        if (!permissions.contains(permission)) {
            throw new ApiException(403, "Forbidden");
        }
    }
}

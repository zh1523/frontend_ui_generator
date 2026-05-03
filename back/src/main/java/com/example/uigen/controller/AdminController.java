package com.example.uigen.controller;

import com.example.uigen.common.PagedResponse;
import com.example.uigen.model.dto.AdminAuditLogResponse;
import com.example.uigen.model.dto.AdminUserSummaryResponse;
import com.example.uigen.model.dto.UpdateUserRoleRequest;
import com.example.uigen.model.dto.UpdateUserStatusRequest;
import com.example.uigen.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public PagedResponse<AdminUserSummaryResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listUsers(keyword, page, size);
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserSummaryResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return adminUserService.updateUserRole(userId, request);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserSummaryResponse updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return adminUserService.updateUserStatus(userId, request);
    }

    @GetMapping("/audits")
    public PagedResponse<AdminAuditLogResponse> listAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listAudits(page, size);
    }
}

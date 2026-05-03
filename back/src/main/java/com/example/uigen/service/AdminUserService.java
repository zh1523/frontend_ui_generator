package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.common.PagedResponse;
import com.example.uigen.model.dto.AdminAuditLogResponse;
import com.example.uigen.model.dto.AdminUserSummaryResponse;
import com.example.uigen.model.dto.UpdateUserRoleRequest;
import com.example.uigen.model.dto.UpdateUserStatusRequest;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.entity.AuditLog;
import com.example.uigen.model.enums.Permission;
import com.example.uigen.model.enums.UserRole;
import com.example.uigen.repository.AppUserRepository;
import com.example.uigen.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdminUserService {

    private final AuthorizationService authorizationService;
    private final AppUserRepository appUserRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminUserService(AuthorizationService authorizationService,
                            AppUserRepository appUserRepository,
                            AuditLogRepository auditLogRepository) {
        this.authorizationService = authorizationService;
        this.appUserRepository = appUserRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserSummaryResponse> listUsers(String keyword, int page, int size) {
        authorizationService.requirePermission(Permission.USER_MANAGE);
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<AppUser> userPage;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            userPage = appUserRepository.findAll(pageRequest);
        } else {
            userPage = appUserRepository.findByUsernameContainingIgnoreCase(normalizedKeyword, pageRequest);
        }
        List<AdminUserSummaryResponse> items = userPage.getContent().stream()
                .map(this::toAdminUserSummary)
                .toList();
        return new PagedResponse<>(items, userPage.getTotalElements(), userPage.getNumber(), userPage.getSize());
    }

    @Transactional
    public AdminUserSummaryResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        authorizationService.requirePermission(Permission.USER_MANAGE);
        AppUser actor = authorizationService.requireCurrentUser();
        AppUser target = requireUser(userId);

        if (actor.getId().equals(target.getId())) {
            throw new ApiException(400, "Cannot change your own role");
        }
        if (target.getRole() == request.role()) {
            return toAdminUserSummary(target);
        }
        if (target.getRole() == UserRole.ADMIN
                && request.role() != UserRole.ADMIN
                && Boolean.TRUE.equals(target.getEnabled())) {
            ensureAnotherEnabledAdminExists();
        }

        UserRole beforeRole = target.getRole();
        target.setRole(request.role());
        target.setUpdatedAt(Instant.now());
        appUserRepository.save(target);
        saveAudit(actor, target, "USER_ROLE_UPDATED",
                "role: " + beforeRole.name() + " -> " + request.role().name());
        return toAdminUserSummary(target);
    }

    @Transactional
    public AdminUserSummaryResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        authorizationService.requirePermission(Permission.USER_MANAGE);
        AppUser actor = authorizationService.requireCurrentUser();
        AppUser target = requireUser(userId);
        boolean enabled = Boolean.TRUE.equals(request.enabled());

        if (!enabled && actor.getId().equals(target.getId())) {
            throw new ApiException(400, "Cannot disable yourself");
        }
        if (target.getRole() == UserRole.ADMIN && Boolean.TRUE.equals(target.getEnabled()) && !enabled) {
            ensureAnotherEnabledAdminExists();
        }
        if (Boolean.TRUE.equals(target.getEnabled()) == enabled) {
            return toAdminUserSummary(target);
        }

        boolean beforeEnabled = Boolean.TRUE.equals(target.getEnabled());
        target.setEnabled(enabled);
        target.setUpdatedAt(Instant.now());
        appUserRepository.save(target);
        saveAudit(actor, target, "USER_STATUS_UPDATED",
                "enabled: " + beforeEnabled + " -> " + enabled);
        return toAdminUserSummary(target);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAuditLogResponse> listAudits(int page, int size) {
        authorizationService.requirePermission(Permission.AUDIT_READ);
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<AuditLog> auditPage = auditLogRepository.findAll(pageRequest);
        List<AdminAuditLogResponse> items = auditPage.getContent().stream()
                .map(this::toAuditResponse)
                .toList();
        return new PagedResponse<>(items, auditPage.getTotalElements(), auditPage.getNumber(), auditPage.getSize());
    }

    private AppUser requireUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "User not found"));
    }

    private void ensureAnotherEnabledAdminExists() {
        long enabledAdminCount = appUserRepository.countByRoleAndEnabled(UserRole.ADMIN, true);
        if (enabledAdminCount <= 1) {
            throw new ApiException(409, "At least one enabled admin must remain");
        }
    }

    private void saveAudit(AppUser actor, AppUser target, String action, String detail) {
        AuditLog log = new AuditLog();
        log.setActorUser(actor);
        log.setTargetUser(target);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    private AdminUserSummaryResponse toAdminUserSummary(AppUser user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                Boolean.TRUE.equals(user.getEnabled()),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }

    private AdminAuditLogResponse toAuditResponse(AuditLog log) {
        AppUser targetUser = log.getTargetUser();
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorUser().getId(),
                log.getActorUser().getUsername(),
                log.getAction(),
                targetUser == null ? null : targetUser.getId(),
                targetUser == null ? null : targetUser.getUsername(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}

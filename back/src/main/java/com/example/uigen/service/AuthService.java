package com.example.uigen.service;

import com.example.uigen.common.ApiException;
import com.example.uigen.config.AppProperties;
import com.example.uigen.model.dto.AuthLoginRequest;
import com.example.uigen.model.dto.AuthRegisterRequest;
import com.example.uigen.model.dto.AuthResponse;
import com.example.uigen.model.dto.UserProfileResponse;
import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.entity.UserSession;
import com.example.uigen.model.enums.UserRole;
import com.example.uigen.repository.AppUserRepository;
import com.example.uigen.repository.UserSessionRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final AppProperties appProperties;

    public AuthService(AppUserRepository appUserRepository,
                       UserSessionRepository userSessionRepository,
                       AppProperties appProperties) {
        this.appUserRepository = appUserRepository;
        this.userSessionRepository = userSessionRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new ApiException(409, "Username already exists");
        }
        Instant now = Instant.now();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        UserRole role = appUserRepository.countByRoleAndEnabled(UserRole.ADMIN, true) == 0
                ? UserRole.ADMIN
                : UserRole.USER;
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(now);
        appUserRepository.save(user);
        return createSessionResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        String username = request.username().trim().toLowerCase();
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "Invalid username or password"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(403, "User is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(401, "Invalid username or password");
        }
        Instant now = Instant.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        appUserRepository.save(user);
        return createSessionResponse(user);
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        userSessionRepository.findByToken(token.trim()).ifPresent(userSessionRepository::delete);
    }

    @Transactional
    public AppUser requireUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing X-Auth-Token");
        }
        UserSession session = userSessionRepository.findByToken(token.trim())
                .orElseThrow(() -> new ApiException(401, "Invalid auth token"));
        if (session.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.delete(session);
            throw new ApiException(401, "Auth token expired");
        }
        AppUser user = session.getUser();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(403, "User is disabled");
        }
        session.setLastActiveAt(Instant.now());
        return user;
    }

    public UserProfileResponse toProfile(AppUser user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getRole());
    }

    @Transactional
    @Scheduled(fixedDelay = 3600000L)
    public void cleanupExpiredSessions() {
        userSessionRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private AuthResponse createSessionResponse(AppUser user) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setToken(generateToken());
        session.setCreatedAt(Instant.now());
        session.setLastActiveAt(Instant.now());
        int ttlHours = appProperties.auth() == null || appProperties.auth().sessionTtlHours() == null
                ? 72
                : appProperties.auth().sessionTtlHours();
        session.setExpiresAt(Instant.now().plusSeconds(ttlHours * 3600L));
        userSessionRepository.save(session);
        return new AuthResponse(
                session.getToken(),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                session.getExpiresAt()
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

package com.example.uigen.repository;

import com.example.uigen.model.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByToken(String token);

    void deleteByExpiresAtBefore(Instant now);
}

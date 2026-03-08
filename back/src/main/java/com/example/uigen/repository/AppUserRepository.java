package com.example.uigen.repository;

import com.example.uigen.model.entity.AppUser;
import com.example.uigen.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Page<AppUser> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    long countByRoleAndEnabled(UserRole role, Boolean enabled);
}

package com.example.uigen.repository;

import com.example.uigen.model.entity.ProjectSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSpaceRepository extends JpaRepository<ProjectSpace, Long> {
    List<ProjectSpace> findByOwnerUserIdAndArchivedFalseOrderByUpdatedAtDesc(Long ownerUserId);

    Optional<ProjectSpace> findByIdAndOwnerUserIdAndArchivedFalse(Long id, Long ownerUserId);
}

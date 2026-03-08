package com.example.uigen.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComponentVersionRepository extends JpaRepository<ComponentVersion, Long> {
    List<ComponentVersion> findByTaskIdOrderByVersionNoDesc(Long taskId);

    Optional<ComponentVersion> findTopByTaskIdOrderByVersionNoDesc(Long taskId);

    @Query("select coalesce(max(v.versionNo), 0) from ComponentVersion v where v.task.id = :taskId")
    Integer findMaxVersionNo(@Param("taskId") Long taskId);
}

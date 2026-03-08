package com.example.uigen.repository;

import com.example.uigen.model.entity.LlmCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {
    @Query("""
            select coalesce(sum(l.totalTokens), 0)
            from LlmCallLog l
            where l.task.project.ownerUser.id = :userId
              and l.createdAt >= :start
              and l.createdAt < :end
            """)
    long sumTotalTokensByUserAndTimeRange(@Param("userId") Long userId,
                                          @Param("start") Instant start,
                                          @Param("end") Instant end);

    @Query("""
            select coalesce(sum(l.estimatedCostUsd), 0)
            from LlmCallLog l
            where l.task.project.ownerUser.id = :userId
              and l.createdAt >= :start
              and l.createdAt < :end
            """)
    double sumCostByUserAndTimeRange(@Param("userId") Long userId,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end);
}

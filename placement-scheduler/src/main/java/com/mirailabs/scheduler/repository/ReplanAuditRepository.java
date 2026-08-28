package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.ReplanAudit;
import com.mirailabs.scheduler.entity.ReplanDisruptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReplanAuditRepository
        extends JpaRepository<ReplanAudit, Long> {

    long countByMovedTrue();

    long countByMovedTrueAndReplannedAtAfter(
            LocalDateTime after
    );

    List<ReplanAudit> findByDisruptionType(
            ReplanDisruptionType disruptionType
    );

    List<ReplanAudit> findAllByOrderByReplannedAtDesc();

    List<ReplanAudit> findByReplannedAtBetweenOrderByReplannedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<ReplanAudit>
    findByDisruptionTypeOrderByReplannedAtDesc(
            ReplanDisruptionType disruptionType
    );


}
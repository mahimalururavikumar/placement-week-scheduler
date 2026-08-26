package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.CandidateDecision;
import com.mirailabs.scheduler.entity.CandidateDecisionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateDecisionRepository
        extends JpaRepository<CandidateDecision, Long> {

    List<CandidateDecision> findByReason(
            CandidateDecisionReason reason
    );

    List<CandidateDecision> findByCompanyId(
            Long companyId
    );

    List<CandidateDecision> findByStudentId(
            Long studentId
    );
}
package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.Shortlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortlistRepository extends JpaRepository<Shortlist, Long> {

    boolean existsByStudentIdAndCompanyId(
            Long studentId,
            Long companyId
    );

    List<Shortlist> findByStudentId(Long studentId);

    List<Shortlist> findByCompanyId(Long companyId);
}
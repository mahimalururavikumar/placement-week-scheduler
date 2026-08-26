package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.CompanySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CompanySlotRepository extends JpaRepository<CompanySlot, Long> {

    List<CompanySlot> findByCompanyId(Long companyId);

    List<CompanySlot> findByCompanyIdAndDateAndActiveTrue(
            Long companyId,
            LocalDate date
    );

    List<CompanySlot> findByCompanyIdAndActiveTrue(Long companyId);
}
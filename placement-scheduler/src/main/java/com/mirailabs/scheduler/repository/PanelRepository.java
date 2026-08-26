package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.Panel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PanelRepository extends JpaRepository<Panel, Long> {

    List<Panel> findByCompanyId(Long companyId);

    List<Panel> findByCompanyIdAndActiveTrue(Long companyId);
}
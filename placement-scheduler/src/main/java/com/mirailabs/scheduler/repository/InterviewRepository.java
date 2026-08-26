package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.Interview;
import com.mirailabs.scheduler.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByStudentId(Long studentId);

    List<Interview> findByCompanyId(Long companyId);

    List<Interview> findByStatus(InterviewStatus status);

    List<Interview> findByRoomId(Long roomId);

    List<Interview> findByPanelId(Long panelId);
}
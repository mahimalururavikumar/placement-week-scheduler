package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.Student;
import com.mirailabs.scheduler.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentCode(String studentCode);
    long countByStatus(StudentStatus status);
}
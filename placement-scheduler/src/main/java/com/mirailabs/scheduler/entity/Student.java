package com.mirailabs.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String studentCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private  Double cgpa;

    @Column(nullable = false)
    private  String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private  StudentStatus status;
}

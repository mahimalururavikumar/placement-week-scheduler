package com.mirailabs.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "shortlists",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_company",
                        columnNames = {"student_id", "company_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shortlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
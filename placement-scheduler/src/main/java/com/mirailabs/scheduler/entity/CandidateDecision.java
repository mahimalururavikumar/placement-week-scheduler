package com.mirailabs.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "candidate_decisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_candidate_decision",
                        columnNames = {
                                "student_id",
                                "company_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateDecisionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateDecisionReason reason;
}
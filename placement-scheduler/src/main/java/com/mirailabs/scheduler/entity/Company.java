package com.mirailabs.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private  String companyCode;

    @Column(nullable = false)
    private  String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private  PriorityTier priorityTier;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal cgpaCutoff;

    @Column(nullable = false)
    private Integer interviewDurationMinutes;

    @Column(nullable = false)
    private Boolean active;
}

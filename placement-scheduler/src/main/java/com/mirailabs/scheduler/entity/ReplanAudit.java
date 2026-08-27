package com.mirailabs.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "replan_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplanAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long interviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplanDisruptionType disruptionType;

    @Column(nullable = false)
    private LocalDateTime replannedAt;

    private LocalDate oldDate;

    private LocalTime oldStartTime;

    private LocalTime oldEndTime;

    private Long oldRoomId;

    private Long oldPanelId;

    private LocalDate newDate;

    private LocalTime newStartTime;

    private LocalTime newEndTime;

    private Long newRoomId;

    private Long newPanelId;

    @Column(nullable = false)
    private boolean moved;

    @Column(nullable = false)
    private boolean cancelled;

    @Column(length = 500)
    private String reason;
}
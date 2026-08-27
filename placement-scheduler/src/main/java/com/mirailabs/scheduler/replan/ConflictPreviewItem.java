package com.mirailabs.scheduler.replan;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConflictPreviewItem(

        Long interviewId,

        Long studentId,
        String studentCode,
        String studentName,

        Long companyId,
        String companyCode,
        String companyName,

        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,

        Long roomId,
        String roomCode,

        Long panelId,
        String panelCode
) {
}
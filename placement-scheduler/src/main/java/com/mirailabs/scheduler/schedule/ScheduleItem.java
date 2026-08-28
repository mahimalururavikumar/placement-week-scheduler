package com.mirailabs.scheduler.schedule;

import com.mirailabs.scheduler.entity.InterviewStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleItem(

        Long interviewId,

        Long studentId,
        String studentCode,
        String studentName,

        Long companyId,
        String companyCode,
        String companyName,
        String priorityTier,

        Long panelId,
        String panelCode,

        Long roomId,
        String roomCode,

        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,

        InterviewStatus status
) {
}
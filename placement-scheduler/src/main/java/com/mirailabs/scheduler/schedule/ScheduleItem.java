package com.mirailabs.scheduler.schedule;

import com.mirailabs.scheduler.entity.InterviewStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleItem(

        Long interviewId,

        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,

        String studentCode,
        String studentName,

        String companyCode,
        String companyName,

        String priorityTier,

        String panelCode,
        String roomCode,

        InterviewStatus status
) {
}
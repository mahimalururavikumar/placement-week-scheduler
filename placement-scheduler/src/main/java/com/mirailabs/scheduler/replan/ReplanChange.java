package com.mirailabs.scheduler.replan;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReplanChange(

        Long interviewId,

        Long studentId,
        String studentCode,

        Long companyId,
        String companyCode,

        LocalDate oldDate,
        LocalTime oldStartTime,
        LocalTime oldEndTime,

        Long oldRoomId,
        Long oldPanelId,

        LocalDate newDate,
        LocalTime newStartTime,
        LocalTime newEndTime,

        Long newRoomId,
        Long newPanelId,

        String reason
) {
}
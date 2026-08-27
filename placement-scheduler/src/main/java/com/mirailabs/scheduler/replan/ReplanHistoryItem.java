package com.mirailabs.scheduler.replan;

import com.mirailabs.scheduler.entity.ReplanDisruptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReplanHistoryItem(

        Long auditId,
        Long interviewId,

        ReplanDisruptionType disruptionType,
        LocalDateTime replannedAt,

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

        boolean moved,
        boolean cancelled,

        String reason
) {
}
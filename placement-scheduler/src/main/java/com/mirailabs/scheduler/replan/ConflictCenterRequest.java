package com.mirailabs.scheduler.replan;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConflictCenterRequest(

        String conflictType,

        Long roomId,

        Long panelId,

        Long companyId,

        Long studentId,

        LocalDate date,

        LocalTime newStartTime

) {
}
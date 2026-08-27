package com.mirailabs.scheduler.replan;

import java.time.LocalTime;
import java.util.List;

public record CompanyDelayResult(

        Long companyId,

        String companyCode,

        String companyName,

        LocalTime oldStartTime,

        LocalTime newStartTime,

        LocalTime endTime,

        int affectedAppointments,

        int movedAppointments,

        int unscheduledAppointments,

        int unchangedAppointments,

        List<ReplanChange> changes
) {
}
package com.mirailabs.scheduler.replan;

import java.util.List;

public record StudentWithdrawResult(

        Long studentId,
        String studentCode,
        String studentName,

        int affectedAppointments,
        int cancelledAppointments,
        int unchangedAppointments,

        List<ReplanChange> changes
) {
}
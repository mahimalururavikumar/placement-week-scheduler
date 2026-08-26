package com.mirailabs.scheduler.replan;

import java.util.List;

public record ReplanResult(

        Long roomId,

        String roomCode,

        int affectedAppointments,

        int movedAppointments,

        int unscheduledAppointments,

        int unchangedAppointments,

        List<ReplanChange> changes
) {
}
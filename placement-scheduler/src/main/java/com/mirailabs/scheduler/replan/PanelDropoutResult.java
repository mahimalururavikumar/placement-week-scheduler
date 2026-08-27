package com.mirailabs.scheduler.replan;

import java.util.List;

public record PanelDropoutResult(

        Long panelId,

        String panelCode,

        int affectedAppointments,

        int movedAppointments,

        int unscheduledAppointments,

        int unchangedAppointments,

        List<ReplanChange> changes
) {
}
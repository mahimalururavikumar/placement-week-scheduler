package com.mirailabs.scheduler.replan;

public record ConflictSummary(

        String conflictType,

        int affectedAppointments,

        int movedAppointments,

        int unscheduledAppointments,

        int cancelledAppointments,

        int unchangedAppointments,

        int totalChanges

) {
}
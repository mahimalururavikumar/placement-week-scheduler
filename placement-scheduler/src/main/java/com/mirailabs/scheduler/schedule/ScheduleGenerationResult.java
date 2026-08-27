package com.mirailabs.scheduler.schedule;

public record ScheduleGenerationResult(
        boolean success,
        long totalCandidates,
        long scheduled,
        long unscheduled,
        String message
) {
}
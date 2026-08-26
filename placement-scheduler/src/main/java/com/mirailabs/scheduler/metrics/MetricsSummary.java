package com.mirailabs.scheduler.metrics;

public record MetricsSummary(

        long totalInterviewCandidates,

        long scheduledInterviews,

        long unscheduledInterviews,

        long totalActiveStudents,

        long studentsWithInterview,

        long studentsWithoutInterview,

        long studentsWithMultipleInterviews,

        double schedulingSuccessPercentage,

        double studentCoveragePercentage,

        double roomUtilizationPercentage,

        double panelUtilizationPercentage,

        double averageStudentWaitingMinutes,

        long replanMovedAppointments
) {
}
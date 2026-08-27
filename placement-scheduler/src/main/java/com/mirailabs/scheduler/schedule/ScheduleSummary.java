package com.mirailabs.scheduler.schedule;

public record ScheduleSummary(

        long totalScheduled,

        long totalUnscheduled,

        long totalStudents,

        long studentsWithInterview,

        long studentsWithoutInterview,

        long multipleInterviewStudents,

        double schedulingSuccessPercentage

) {
}
package com.mirailabs.scheduler.scheduler;

import com.mirailabs.scheduler.entity.Panel;
import com.mirailabs.scheduler.entity.Room;

import java.time.LocalDate;
import java.time.LocalTime;

public record SchedulingCandidate(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Room room,
        Panel panel
) {
}
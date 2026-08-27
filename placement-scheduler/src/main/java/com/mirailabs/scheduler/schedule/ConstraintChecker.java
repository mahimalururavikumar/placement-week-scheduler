package com.mirailabs.scheduler.schedule;

import com.mirailabs.scheduler.entity.CompanySlot;
import com.mirailabs.scheduler.entity.Interview;
import com.mirailabs.scheduler.entity.Panel;
import com.mirailabs.scheduler.entity.Room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ConstraintChecker {

    public boolean hasStudentConflict(
            Interview interview,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<Interview> scheduledInterviews) {

        return scheduledInterviews.stream()
                .filter(existing ->
                        existing.getStudent().getId()
                                .equals(interview.getStudent().getId()))
                .anyMatch(existing ->
                        overlaps(
                                existing.getDate(),
                                existing.getStartTime(),
                                existing.getEndTime(),
                                date,
                                startTime,
                                endTime
                        ));
    }

    public boolean hasRoomConflict(
            Room room,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<Interview> scheduledInterviews) {

        return scheduledInterviews.stream()
                .filter(existing ->
                        existing.getRoom() != null
                                && existing.getRoom().getId()
                                .equals(room.getId()))
                .anyMatch(existing ->
                        overlaps(
                                existing.getDate(),
                                existing.getStartTime(),
                                existing.getEndTime(),
                                date,
                                startTime,
                                endTime
                        ));
    }

    public boolean hasPanelConflict(
            Panel panel,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<Interview> scheduledInterviews) {

        return scheduledInterviews.stream()
                .filter(existing ->
                        existing.getPanel() != null
                                && existing.getPanel()
                                .getId()
                                .equals(panel.getId())
                )
                .anyMatch(existing ->
                        overlaps(
                                existing.getDate(),
                                existing.getStartTime(),
                                existing.getEndTime(),
                                date,
                                startTime,
                                endTime
                        )
                );
    }

    public boolean isWithinCompanySlot(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<CompanySlot> companySlots) {

        return companySlots.stream()
                .filter(slot ->
                        slot.getDate().equals(date)
                                && Boolean.TRUE.equals(slot.getActive()))
                .anyMatch(slot ->
                        !startTime.isBefore(slot.getStartTime())
                                && !endTime.isAfter(slot.getEndTime()));
    }

    private boolean overlaps(
            LocalDate existingDate,
            LocalTime existingStart,
            LocalTime existingEnd,
            LocalDate newDate,
            LocalTime newStart,
            LocalTime newEnd) {

        if (existingDate == null
                || existingStart == null
                || existingEnd == null) {
            return false;
        }

        if (!existingDate.equals(newDate)) {
            return false;
        }

        return existingStart.isBefore(newEnd)
                && existingEnd.isAfter(newStart);
    }
}
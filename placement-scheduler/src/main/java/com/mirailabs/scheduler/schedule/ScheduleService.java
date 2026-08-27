package com.mirailabs.scheduler.schedule;

import com.mirailabs.scheduler.entity.Interview;
import com.mirailabs.scheduler.entity.InterviewStatus;
import com.mirailabs.scheduler.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final InterviewRepository interviewRepository;

    @Transactional(readOnly = true)
    public List<ScheduleItem> getSchedule(
            LocalDate date,
            Long companyId,
            Long studentId,
            Long roomId,
            Long panelId) {

        return interviewRepository
                .findByStatus(InterviewStatus.SCHEDULED)
                .stream()

                .filter(interview ->
                        date == null
                                || date.equals(interview.getDate()))

                .filter(interview ->
                        companyId == null
                                || companyId.equals(
                                interview.getCompany().getId()))

                .filter(interview ->
                        studentId == null
                                || studentId.equals(
                                interview.getStudent().getId()))

                .filter(interview ->
                        roomId == null
                                || (
                                interview.getRoom() != null
                                        && roomId.equals(
                                        interview.getRoom().getId())
                        ))

                .filter(interview ->
                        panelId == null
                                || (
                                interview.getPanel() != null
                                        && panelId.equals(
                                        interview.getPanel().getId())
                        ))

                .sorted(
                        Comparator
                                .comparing(
                                        Interview::getDate
                                )
                                .thenComparing(
                                        Interview::getStartTime
                                )
                                .thenComparing(
                                        interview ->
                                                interview.getRoom()
                                                        .getRoomCode()
                                )
                )

                .map(this::toScheduleItem)
                .toList();
    }

    private ScheduleItem toScheduleItem(
            Interview interview) {

        return new ScheduleItem(

                interview.getId(),

                interview.getDate(),
                interview.getStartTime(),
                interview.getEndTime(),

                interview.getStudent()
                        .getStudentCode(),

                interview.getStudent()
                        .getName(),

                interview.getCompany()
                        .getCompanyCode(),

                interview.getCompany()
                        .getName(),

                interview.getCompany()
                        .getPriorityTier()
                        .name(),

                interview.getPanel()
                        .getPanelCode(),

                interview.getRoom()
                        .getRoomCode(),

                interview.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public ScheduleSummary getScheduleSummary() {

        List<com.mirailabs.scheduler.entity.Interview> scheduled =
                interviewRepository.findByStatus(
                        InterviewStatus.SCHEDULED
                );

        List<com.mirailabs.scheduler.entity.Interview> allInterviews =
                interviewRepository.findAll();

        long totalScheduled =
                scheduled.size();

        long totalUnscheduled =
                allInterviews.stream()
                        .filter(interview ->
                                interview.getStatus()
                                        == InterviewStatus.UNSCHEDULED)
                        .count();

        long totalStudents =
                allInterviews.stream()
                        .filter(interview ->
                                interview.getStudent() != null)
                        .map(interview ->
                                interview.getStudent().getId())
                        .distinct()
                        .count();

        long studentsWithInterview =
                scheduled.stream()
                        .filter(interview ->
                                interview.getStudent() != null)
                        .map(interview ->
                                interview.getStudent().getId())
                        .distinct()
                        .count();

        long studentsWithoutInterview =
                Math.max(
                        totalStudents - studentsWithInterview,
                        0
                );

        long multipleInterviewStudents =
                scheduled.stream()
                        .filter(interview ->
                                interview.getStudent() != null)
                        .collect(
                                java.util.stream.Collectors
                                        .groupingBy(
                                                interview ->
                                                        interview
                                                                .getStudent()
                                                                .getId(),
                                                java.util.stream.Collectors
                                                        .counting()
                                        )
                        )
                        .values()
                        .stream()
                        .filter(count -> count > 1)
                        .count();

        long totalCandidates =
                totalScheduled + totalUnscheduled;

        double schedulingSuccessPercentage =
                totalCandidates == 0
                        ? 0.0
                        : Math.round(
                        (
                                totalScheduled * 100.0
                        )
                                / totalCandidates
                                * 100.0
                ) / 100.0;

        return new ScheduleSummary(
                totalScheduled,
                totalUnscheduled,
                totalStudents,
                studentsWithInterview,
                studentsWithoutInterview,
                multipleInterviewStudents,
                schedulingSuccessPercentage
        );
    }


}
package com.mirailabs.scheduler.metrics;

import com.mirailabs.scheduler.config.DatasetConfig;
import com.mirailabs.scheduler.entity.CompanySlot;
import com.mirailabs.scheduler.entity.Interview;
import com.mirailabs.scheduler.entity.InterviewStatus;
import com.mirailabs.scheduler.entity.StudentStatus;
import com.mirailabs.scheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final InterviewRepository interviewRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final CompanySlotRepository companySlotRepository;
    private final ReplanAuditRepository replanAuditRepository;

    public MetricsSummary calculateSummary() {

        long start = System.currentTimeMillis();

        System.out.println("METRICS: started");

        List<Interview> interviews =
                interviewRepository.findAll();

        System.out.println(
                "METRICS: findAll = "
                        + (System.currentTimeMillis() - start)
                        + " ms"
        );

        List<Interview> scheduled =
                interviews.stream()
                        .filter(interview ->
                                interview.getStatus()
                                        == InterviewStatus.SCHEDULED)
                        .toList();

        System.out.println(
                "METRICS: scheduled filter = "
                        + (System.currentTimeMillis() - start)
                        + " ms"
        );

        long activeStudents;

        long studentsWithInterview;

        long totalCandidates = interviews.size();

        long scheduledCount = scheduled.size();

        long studentsWithMultipleInterviews =
                scheduled.stream()
                        .collect(Collectors.groupingBy(
                                interview ->
                                        interview.getStudent().getId(),
                                Collectors.counting()
                        ))
                        .values()
                        .stream()
                        .filter(count -> count > 1)
                        .count();

        long unscheduledCount =
                interviews.stream()
                        .filter(interview ->
                                interview.getStatus()
                                        == InterviewStatus.UNSCHEDULED)
                        .count();

        activeStudents = studentRepository.countByStatus(
                StudentStatus.ACTIVE
        );

        studentsWithInterview = scheduled.stream()
                .map(interview ->
                        interview.getStudent().getId())
                .distinct()
                .count();

        double schedulingSuccess =
                percentage(
                        scheduledCount,
                        totalCandidates
                );

        double studentCoverage =
                percentage(
                        studentsWithInterview,
                        activeStudents
                );

        double roomUtilization =
                calculateRoomUtilization(scheduled);

        double panelUtilization =
                calculatePanelUtilization(scheduled);

        double averageWaiting =
                calculateAverageWaitingTime(scheduled);

        long studentsWithoutInterview =
                Math.max(
                        0,
                        activeStudents - studentsWithInterview
                );
        long replanMovedAppointments =
                replanAuditRepository.countByMovedTrue();

        return new MetricsSummary(
                totalCandidates,
                scheduledCount,
                unscheduledCount,
                activeStudents,
                studentsWithInterview,
                studentsWithMultipleInterviews,
                studentsWithoutInterview,
                schedulingSuccess,
                studentCoverage,
                roomUtilization,
                panelUtilization,
                averageWaiting,
                replanMovedAppointments
        );
    }

    private double percentage(
            long numerator,
            long denominator) {

        if (denominator == 0) {
            return 0.0;
        }

        return Math.round(
                ((double) numerator / denominator) * 10000.0
        ) / 100.0;
    }


    private double calculateAverageWaitingTime(
            List<Interview> scheduled) {

        if (scheduled.isEmpty()) {
            return 0.0;
        }

        Map<Long, List<Interview>> interviewsByStudent =
                scheduled.stream()
                        .collect(Collectors.groupingBy(
                                interview ->
                                        interview.getStudent().getId()
                        ));

        long totalWaitingMinutes = 0;
        long waitingIntervals = 0;

        for (List<Interview> studentInterviews
                : interviewsByStudent.values()) {

            studentInterviews.sort(
                    Comparator
                            .comparing(Interview::getDate)
                            .thenComparing(Interview::getStartTime)
            );

            for (int i = 1;
                 i < studentInterviews.size();
                 i++) {

                Interview previous =
                        studentInterviews.get(i - 1);

                Interview current =
                        studentInterviews.get(i);

                /*
                 * Do not count overnight gaps as student waiting time.
                 */
                if (!previous.getDate()
                        .equals(current.getDate())) {

                    continue;
                }

                long waitingMinutes =
                        Duration.between(
                                previous.getEndTime(),
                                current.getStartTime()
                        ).toMinutes();

                if (waitingMinutes >= 0) {
                    totalWaitingMinutes += waitingMinutes;
                    waitingIntervals++;
                }
            }
        }

        if (waitingIntervals == 0) {
            return 0.0;
        }

        return Math.round(
                ((double) totalWaitingMinutes
                        / waitingIntervals) * 100.0
        ) / 100.0;
    }

    private double calculateRoomUtilization(
            List<Interview> scheduled) {

        if (scheduled.isEmpty()) {
            return 0.0;
        }

        long occupiedMinutes =
                scheduled.stream()
                        .filter(interview ->
                                interview.getRoom() != null)
                        .mapToLong(interview ->
                                Duration.between(
                                        interview.getStartTime(),
                                        interview.getEndTime()
                                ).toMinutes()
                        )
                        .sum();

        long availableMinutes =
                (long) roomRepository.countByActiveTrue()
                        * DatasetConfig.PLACEMENT_DATES.size()
                        * Duration.between(
                        DatasetConfig.DEFAULT_START_TIME,
                        DatasetConfig.DEFAULT_END_TIME
                ).toMinutes();

        if (availableMinutes == 0) {
            return 0.0;
        }

        return Math.round(
                ((double) occupiedMinutes
                        / availableMinutes) * 10000.0
        ) / 100.0;
    }

    private double calculatePanelUtilization(
            List<Interview> scheduled) {

        if (scheduled.isEmpty()) {
            return 0.0;
        }

        long occupiedMinutes =
                scheduled.stream()
                        .filter(interview ->
                                interview.getPanel() != null)
                        .mapToLong(interview ->
                                Duration.between(
                                        interview.getStartTime(),
                                        interview.getEndTime()
                                ).toMinutes()
                        )
                        .sum();

        List<CompanySlot> activeSlots =
                companySlotRepository.findAll()
                        .stream()
                        .filter(slot ->
                                Boolean.TRUE.equals(
                                        slot.getActive()))
                        .toList();

        long availableMinutes = 0;

        for (CompanySlot slot : activeSlots) {

            long slotMinutes =
                    Duration.between(
                            slot.getStartTime(),
                            slot.getEndTime()
                    ).toMinutes();

            long activePanels =
                    panelRepository
                            .findByCompanyIdAndActiveTrue(
                                    slot.getCompany().getId()
                            )
                            .size();

            availableMinutes +=
                    slotMinutes * activePanels;
        }

        if (availableMinutes == 0) {
            return 0.0;
        }

        return Math.round(
                ((double) occupiedMinutes
                        / availableMinutes) * 10000.0
        ) / 100.0;
    }
}
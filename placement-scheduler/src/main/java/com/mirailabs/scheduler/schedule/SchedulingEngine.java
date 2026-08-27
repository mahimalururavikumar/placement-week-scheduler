package com.mirailabs.scheduler.schedule;

import com.mirailabs.scheduler.entity.*;
import com.mirailabs.scheduler.repository.CompanySlotRepository;
import com.mirailabs.scheduler.repository.InterviewRepository;
import com.mirailabs.scheduler.repository.PanelRepository;
import com.mirailabs.scheduler.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchedulingEngine {

    private static final int SLOT_GRANULARITY_MINUTES = 15;

    private final InterviewRepository interviewRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final CompanySlotRepository companySlotRepository;

    private final ConstraintChecker constraintChecker =
            new ConstraintChecker();
    private final Map<Long, Integer> companyAvailabilityDays =
            new HashMap<>();

    @Transactional
    public ScheduleGenerationResult generateInitialSchedule() {

        System.out.println("========================================");
        System.out.println("GLOBAL INITIAL SCHEDULER STARTED");
        System.out.println("========================================");

        List<Interview> interviews =
                interviewRepository.findByStatus(
                        InterviewStatus.PENDING
                );

        List<Room> rooms =
                roomRepository.findByActiveTrue();

        List<CompanySlot> allCompanySlots =
                companySlotRepository.findAll()
                        .stream()
                        .filter(slot ->
                                Boolean.TRUE.equals(slot.getActive()))
                        .toList();

        List<Panel> allPanels =
                panelRepository.findAll()
                        .stream()
                        .filter(panel ->
                                Boolean.TRUE.equals(panel.getActive()))
                        .toList();

        Map<Long, List<CompanySlot>> slotsByCompany =
                groupSlotsByCompany(allCompanySlots);

        Map<Long, List<Panel>> panelsByCompany =
                groupPanelsByCompany(allPanels);

        /*
         * These indexes contain only already scheduled interviews.
         */
        Map<Long, List<Interview>> studentSchedule =
                new HashMap<>();

        Map<Long, List<Interview>> roomSchedule =
                new HashMap<>();

        Map<Long, List<Interview>> panelSchedule =
                new HashMap<>();

        /*
         * Remaining interviews that still need processing.
         */
        List<Interview> pendingInterviews =
                new ArrayList<>(interviews);

        int scheduled = 0;
        int unscheduled = 0;

        /*
         * Dynamically choose the next interview.
         */
        while (!pendingInterviews.isEmpty()) {

            Interview interview =
                    selectNextInterview(
                            pendingInterviews,
                            studentSchedule,
                            slotsByCompany
                    );

            if (interview == null) {
                break;
            }

            SchedulingCandidate candidate =
                    findBestCandidate(
                            interview,
                            slotsByCompany.getOrDefault(
                                    interview.getCompany().getId(),
                                    List.of()
                            ),
                            panelsByCompany.getOrDefault(
                                    interview.getCompany().getId(),
                                    List.of()
                            ),
                            rooms,
                            studentSchedule,
                            roomSchedule,
                            panelSchedule
                    );

            if (candidate != null) {

                applyCandidate(
                        interview,
                        candidate
                );

                addToIndex(
                        studentSchedule,
                        interview.getStudent().getId(),
                        interview
                );

                addToIndex(
                        roomSchedule,
                        candidate.room().getId(),
                        interview
                );

                addToIndex(
                        panelSchedule,
                        candidate.panel().getId(),
                        interview
                );

                scheduled++;

            } else {

                interview.setStatus(
                        InterviewStatus.UNSCHEDULED
                );

                interview.setUnscheduledReason(
                        buildUnscheduledReason(
                                interview,
                                slotsByCompany,
                                panelsByCompany
                        )
                );

                unscheduled++;
            }

            /*
             * Remove the processed interview.
             */
            pendingInterviews.remove(interview);

            int processed =
                    scheduled + unscheduled;

            if (processed % 250 == 0) {

                System.out.println(
                        "Processed "
                                + processed
                                + " / "
                                + interviews.size()
                                + " | Scheduled = "
                                + scheduled
                                + " | Unscheduled = "
                                + unscheduled
                );
            }
        }

        interviewRepository.saveAll(interviews);

        System.out.println("========================================");
        System.out.println("GLOBAL INITIAL SCHEDULER FINISHED");
        System.out.println("Scheduled   : " + scheduled);
        System.out.println("Unscheduled : " + unscheduled);
        System.out.println("========================================");

        return new ScheduleGenerationResult(
                true,
                interviews.size(),
                scheduled,
                unscheduled,
                "Initial schedule generated successfully."
        );
    }

    private SchedulingCandidate findBestCandidate(
            Interview interview,
            List<CompanySlot> companySlots,
            List<Panel> panels,
            List<Room> rooms,
            Map<Long, List<Interview>> studentSchedule,
            Map<Long, List<Interview>> roomSchedule,
            Map<Long, List<Interview>> panelSchedule) {

        int duration =
                interview.getCompany()
                        .getInterviewDurationMinutes();

        List<Interview> studentInterviews =
                studentSchedule.getOrDefault(
                        interview.getStudent().getId(),
                        List.of()
                );

        SchedulingCandidate best = null;

        for (CompanySlot slot : companySlots) {

            LocalTime current =
                    slot.getStartTime();

            while (!current.plusMinutes(duration)
                    .isAfter(slot.getEndTime())) {

                LocalTime end =
                        current.plusMinutes(duration);

                /*
                 * HARD CONSTRAINT:
                 * student cannot have overlapping interviews.
                 */
                if (constraintChecker.hasStudentConflict(
                        interview,
                        slot.getDate(),
                        current,
                        end,
                        studentInterviews)) {

                    current = current.plusMinutes(
                            SLOT_GRANULARITY_MINUTES
                    );

                    continue;
                }

                for (Panel panel : panels) {

                    List<Interview> panelInterviews =
                            panelSchedule.getOrDefault(
                                    panel.getId(),
                                    List.of()
                            );

                    /*
                     * HARD CONSTRAINT:
                     * same panel cannot conduct two interviews.
                     */
                    if (constraintChecker.hasPanelConflict(
                            panel,
                            slot.getDate(),
                            current,
                            end,
                            panelInterviews)) {

                        continue;
                    }

                    for (Room room : rooms) {

                        List<Interview> roomInterviews =
                                roomSchedule.getOrDefault(
                                        room.getId(),
                                        List.of()
                                );

                        /*
                         * HARD CONSTRAINT:
                         * same room cannot contain two interviews.
                         */
                        if (constraintChecker.hasRoomConflict(
                                room,
                                slot.getDate(),
                                current,
                                end,
                                roomInterviews)) {

                            continue;
                        }

                        SchedulingCandidate candidate =
                                new SchedulingCandidate(
                                        slot.getDate(),
                                        current,
                                        end,
                                        room,
                                        panel
                                );

                        /*
                         * Don't immediately return.
                         * Compare candidates so we can prefer
                         * better utilization.
                         */
                        if (best == null
                                || isBetterCandidate(
                                candidate,
                                best)) {

                            best = candidate;
                        }
                    }
                }

                current = current.plusMinutes(
                        SLOT_GRANULARITY_MINUTES
                );
            }
        }

        return best;
    }

    private boolean isBetterCandidate(
            SchedulingCandidate candidate,
            SchedulingCandidate currentBest) {

        /*
         * First preference:
         * earlier date.
         */
        int dateComparison =
                candidate.date()
                        .compareTo(currentBest.date());

        if (dateComparison != 0) {
            return dateComparison < 0;
        }

        /*
         * Second preference:
         * earliest start time.
         */
        return candidate.startTime()
                .isBefore(currentBest.startTime());
    }

    private int calculateFlexibility(
            Interview interview,
            Map<Long, List<CompanySlot>> slotsByCompany,
            Map<Long, List<Panel>> panelsByCompany) {

        Long companyId =
                interview.getCompany().getId();

        List<CompanySlot> slots =
                slotsByCompany.getOrDefault(
                        companyId,
                        List.of()
                );

        List<Panel> panels =
                panelsByCompany.getOrDefault(
                        companyId,
                        List.of()
                );

        int duration =
                interview.getCompany()
                        .getInterviewDurationMinutes();

        int possibleIntervals = 0;

        for (CompanySlot slot : slots) {

            LocalTime current =
                    slot.getStartTime();

            while (!current.plusMinutes(duration)
                    .isAfter(slot.getEndTime())) {

                possibleIntervals++;

                current = current.plusMinutes(
                        SLOT_GRANULARITY_MINUTES
                );
            }
        }

        return possibleIntervals
                * Math.max(panels.size(), 1);
    }

    private Map<Long, List<CompanySlot>> groupSlotsByCompany(
            List<CompanySlot> slots) {

        Map<Long, List<CompanySlot>> result =
                new HashMap<>();

        for (CompanySlot slot : slots) {

            result.computeIfAbsent(
                    slot.getCompany().getId(),
                    key -> new ArrayList<>()
            ).add(slot);
        }

        return result;
    }

    private Map<Long, List<Panel>> groupPanelsByCompany(
            List<Panel> panels) {

        Map<Long, List<Panel>> result =
                new HashMap<>();

        for (Panel panel : panels) {

            result.computeIfAbsent(
                    panel.getCompany().getId(),
                    key -> new ArrayList<>()
            ).add(panel);
        }

        return result;
    }

    private void addToIndex(
            Map<Long, List<Interview>> index,
            Long key,
            Interview interview) {

        index.computeIfAbsent(
                key,
                ignored -> new ArrayList<>()
        ).add(interview);
    }

    private void applyCandidate(
            Interview interview,
            SchedulingCandidate candidate) {

        interview.setDate(candidate.date());
        interview.setStartTime(candidate.startTime());
        interview.setEndTime(candidate.endTime());
        interview.setRoom(candidate.room());
        interview.setPanel(candidate.panel());
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setUnscheduledReason(null);
    }

    private String buildUnscheduledReason(
            Interview interview,
            Map<Long, List<CompanySlot>> slotsByCompany,
            Map<Long, List<Panel>> panelsByCompany) {

        Long companyId =
                interview.getCompany().getId();

        List<CompanySlot> slots =
                slotsByCompany.getOrDefault(
                        companyId,
                        List.of()
                );

        List<Panel> panels =
                panelsByCompany.getOrDefault(
                        companyId,
                        List.of()
                );

        if (slots.isEmpty()) {
            return "UNSCHEDULED: Company has no active availability slot.";
        }

        if (panels.isEmpty()) {
            return "UNSCHEDULED: Company has no active panel.";
        }

        return "UNSCHEDULED: No feasible interval remained after applying hard constraints.";
    }

    private int getCompanyAvailabilityDays(
            Interview interview,
            Map<Long, List<CompanySlot>> slotsByCompany) {

        return (int) slotsByCompany
                .getOrDefault(
                        interview.getCompany().getId(),
                        List.of()
                )
                .stream()
                .map(CompanySlot::getDate)
                .distinct()
                .count();
    }

    private Comparator<Interview> buildInterviewComparator(
            Map<Long, List<Interview>> studentSchedule,
            Map<Long, List<CompanySlot>> slotsByCompany) {

        return Comparator
                /*
                 * 1. STUDENT COVERAGE
                 *
                 * Students with fewer interviews
                 * always come first.
                 *
                 * 0 → 1 → 2 → 3...
                 */
                .<Interview>comparingLong(
                        interview ->
                                getScheduledInterviewCount(
                                        interview.getStudent().getId(),
                                        studentSchedule
                                )
                )

                /*
                 * 2. CONSTRAINT / FLEXIBILITY
                 *
                 * Among students with the same number
                 * of interviews, handle the more
                 * constrained company first.
                 */
                .thenComparingInt(
                        interview ->
                                getCompanyAvailabilityDays(
                                        interview,
                                        slotsByCompany
                                )
                )

                /*
                 * 3. TIER PRIORITY
                 *
                 * Only after coverage/flexibility,
                 * prefer TIER_1.
                 */
                .thenComparing(
                        (Interview interview) ->
                                getTierPriority(
                                        interview.getCompany()
                                                .getPriorityTier()
                                ),
                        Comparator.reverseOrder()
                )

                /*
                 * 4. Deterministic ordering.
                 */
                .thenComparingLong(
                        interview ->
                                interview.getStudent().getId()
                )

                .thenComparingLong(
                        Interview::getId
                );
    }

    private long getScheduledInterviewCount(
            Long studentId,
            Map<Long, List<Interview>> studentSchedule) {

        return studentSchedule
                .getOrDefault(studentId, List.of())
                .size();
    }

    private int getTierPriority(PriorityTier tier) {

        return switch (tier) {
            case TIER_1 -> 3;
            case TIER_2 -> 2;
            case TIER_3 -> 1;
        };
    }

    private Interview selectNextInterview(
            List<Interview> pendingInterviews,
            Map<Long, List<Interview>> studentSchedule,
            Map<Long, List<CompanySlot>> slotsByCompany) {

        return pendingInterviews.stream()
                .min(
                        buildInterviewComparator(
                                studentSchedule,
                                slotsByCompany
                        )
                )
                .orElse(null);
    }
}
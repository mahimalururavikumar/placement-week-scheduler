package com.mirailabs.scheduler.replan;

import com.mirailabs.scheduler.entity.CompanySlot;
import com.mirailabs.scheduler.entity.Interview;
import com.mirailabs.scheduler.entity.InterviewStatus;
import com.mirailabs.scheduler.entity.Panel;
import com.mirailabs.scheduler.entity.Room;
import com.mirailabs.scheduler.repository.CompanySlotRepository;
import com.mirailabs.scheduler.repository.InterviewRepository;
import com.mirailabs.scheduler.repository.PanelRepository;
import com.mirailabs.scheduler.repository.RoomRepository;
import com.mirailabs.scheduler.scheduler.ConstraintChecker;
import com.mirailabs.scheduler.scheduler.SchedulingCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplanService {

    private static final int SLOT_GRANULARITY_MINUTES = 15;

    private final InterviewRepository interviewRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final CompanySlotRepository companySlotRepository;

    private final ConstraintChecker constraintChecker =
            new ConstraintChecker();

    @Transactional
    public ReplanResult replanRoomUnavailable(
            RoomUnavailableRequest request) {

        Room unavailableRoom =
                roomRepository.findById(request.roomId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Room not found: "
                                                + request.roomId()
                                )
                        );

        List<Interview> scheduled =
                interviewRepository.findByStatus(
                        InterviewStatus.SCHEDULED
                );

        /*
         * Only interviews using the unavailable room
         * on the affected date are impacted.
         */
        List<Interview> affected =
                scheduled.stream()
                        .filter(interview ->
                                interview.getRoom() != null)
                        .filter(interview ->
                                interview.getRoom()
                                        .getId()
                                        .equals(
                                                request.roomId()
                                        ))
                        .filter(interview ->
                                interview.getDate()
                                        .equals(request.date()))
                        .toList();

        if (affected.isEmpty()) {

            return new ReplanResult(
                    unavailableRoom.getId(),
                    unavailableRoom.getRoomCode(),
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        /*
         * Freeze every unaffected appointment.
         *
         * We NEVER modify these interviews.
         */
        List<Interview> frozen =
                scheduled.stream()
                        .filter(interview ->
                                !affected.contains(interview))
                        .collect(
                                java.util.stream.Collectors.toCollection(
                                        ArrayList::new
                                )
                        );

        /*
         * The affected interviews are processed one by one.
         * Once an affected interview receives a new assignment,
         * it becomes part of the working schedule so another
         * affected interview cannot conflict with it.
         */
        List<Interview> workingSchedule =
                new ArrayList<>(frozen);

        List<ReplanChange> changes =
                new ArrayList<>();

        int moved = 0;
        int unscheduled = 0;

        /*
         * Most constrained / earliest original appointment first.
         * This makes the replan deterministic.
         */
        List<Interview> orderedAffected =
                affected.stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                Interview::getDate
                                        )
                                        .thenComparing(
                                                Interview::getStartTime
                                        )
                                        .thenComparing(
                                                Interview::getId
                                        )
                        )
                        .toList();

        for (Interview interview : orderedAffected) {

            ReplanSnapshot old =
                    ReplanSnapshot.from(interview);

            /*
             * Find the least disruptive feasible replacement.
             */
            SchedulingCandidate candidate =
                    findLeastDisruptiveCandidate(
                            interview,
                            request,
                            workingSchedule
                    );

            if (candidate == null) {

                /*
                 * No valid replacement exists.
                 */
                interview.setRoom(null);
                interview.setPanel(null);
                interview.setDate(null);
                interview.setStartTime(null);
                interview.setEndTime(null);
                interview.setStatus(
                        InterviewStatus.UNSCHEDULED
                );
                interview.setUnscheduledReason(
                        "ROOM_UNAVAILABLE: no feasible replacement slot"
                );

                unscheduled++;

                changes.add(
                        buildChange(
                                interview,
                                old,
                                null,
                                "ROOM_UNAVAILABLE"
                        )
                );

            } else {

                applyCandidate(
                        interview,
                        candidate
                );

                workingSchedule.add(interview);

                moved++;

                changes.add(
                        buildChange(
                                interview,
                                old,
                                candidate,
                                "ROOM_UNAVAILABLE"
                        )
                );
            }
        }

        interviewRepository.saveAll(affected);

        return new ReplanResult(
                unavailableRoom.getId(),
                unavailableRoom.getRoomCode(),
                affected.size(),
                moved,
                unscheduled,
                0,
                changes
        );
    }

    private SchedulingCandidate findLeastDisruptiveCandidate(
            Interview interview,
            RoomUnavailableRequest request,
            List<Interview> workingSchedule) {

        List<CompanySlot> companySlots =
                companySlotRepository
                        .findByCompanyIdAndActiveTrue(
                                interview.getCompany().getId()
                        );

        List<Panel> panels =
                panelRepository
                        .findByCompanyIdAndActiveTrue(
                                interview.getCompany().getId()
                        );

        List<Room> rooms =
                roomRepository.findByActiveTrue()
                        .stream()
                        .filter(room ->
                                !room.getId()
                                        .equals(request.roomId()))
                        .toList();

        List<SchedulingCandidate> candidates =
                new ArrayList<>();

        int duration =
                interview.getCompany()
                        .getInterviewDurationMinutes();

        for (CompanySlot slot : companySlots) {

            LocalTime start =
                    slot.getStartTime();

            while (!start.plusMinutes(duration)
                    .isAfter(slot.getEndTime())) {

                LocalTime end =
                        start.plusMinutes(duration);

                /*
                 * Student conflict.
                 */
                if (constraintChecker.hasStudentConflict(
                        interview,
                        slot.getDate(),
                        start,
                        end,
                        workingSchedule)) {

                    start = start.plusMinutes(
                            SLOT_GRANULARITY_MINUTES
                    );
                    continue;
                }

                for (Panel panel : panels) {

                    if (constraintChecker.hasPanelConflict(
                            panel,
                            slot.getDate(),
                            start,
                            end,
                            workingSchedule)) {
                        continue;
                    }

                    for (Room room : rooms) {

                        if (constraintChecker.hasRoomConflict(
                                room,
                                slot.getDate(),
                                start,
                                end,
                                workingSchedule)) {
                            continue;
                        }

                        candidates.add(
                                new SchedulingCandidate(
                                        slot.getDate(),
                                        start,
                                        end,
                                        room,
                                        panel
                                )
                        );
                    }
                }

                start = start.plusMinutes(
                        SLOT_GRANULARITY_MINUTES
                );
            }
        }

        return candidates.stream()
                .min(
                        Comparator.comparingInt(
                                candidate ->
                                        disruptionCost(
                                                interview,
                                                candidate
                                        )
                        )
                )
                .orElse(null);
    }

    private int disruptionCost(
            Interview interview,
            SchedulingCandidate candidate) {

        int cost = 0;

        /*
         * Prefer keeping the same date.
         */
        if (!candidate.date()
                .equals(interview.getDate())) {

            cost += 10000;
        }

        /*
         * Strongly prefer keeping the same time.
         */
        long timeDifference =
                Math.abs(
                        Duration.between(
                                interview.getStartTime(),
                                candidate.startTime()
                        ).toMinutes()
                );

        cost += (int) timeDifference * 10;

        /*
         * Prefer keeping the same panel.
         */
        if (interview.getPanel() != null
                && !candidate.panel()
                .getId()
                .equals(
                        interview.getPanel().getId()
                )) {

            cost += 100;
        }

        /*
         * Room must change because the old room is unavailable,
         * so there is no penalty here.
         */

        return cost;
    }

    private void applyCandidate(
            Interview interview,
            SchedulingCandidate candidate) {

        interview.setDate(candidate.date());
        interview.setStartTime(candidate.startTime());
        interview.setEndTime(candidate.endTime());
        interview.setRoom(candidate.room());
        interview.setPanel(candidate.panel());
        interview.setStatus(
                InterviewStatus.SCHEDULED
        );
        interview.setUnscheduledReason(null);
    }

    private ReplanChange buildChange(
            Interview interview,
            ReplanSnapshot old,
            SchedulingCandidate candidate,
            String reason) {

        if (candidate == null) {

            return new ReplanChange(
                    interview.getId(),
                    interview.getStudent().getId(),
                    interview.getStudent().getStudentCode(),
                    interview.getCompany().getId(),
                    interview.getCompany().getCompanyCode(),

                    old.date(),
                    old.startTime(),
                    old.endTime(),
                    old.roomId(),
                    old.panelId(),

                    null,
                    null,
                    null,
                    null,
                    null,

                    reason
            );
        }

        return new ReplanChange(
                interview.getId(),
                interview.getStudent().getId(),
                interview.getStudent().getStudentCode(),
                interview.getCompany().getId(),
                interview.getCompany().getCompanyCode(),

                old.date(),
                old.startTime(),
                old.endTime(),
                old.roomId(),
                old.panelId(),

                candidate.date(),
                candidate.startTime(),
                candidate.endTime(),
                candidate.room().getId(),
                candidate.panel().getId(),

                reason
        );
    }

    private record ReplanSnapshot(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Long roomId,
            Long panelId
    ) {

        static ReplanSnapshot from(
                Interview interview) {

            return new ReplanSnapshot(
                    interview.getDate(),
                    interview.getStartTime(),
                    interview.getEndTime(),
                    interview.getRoom() == null
                            ? null
                            : interview.getRoom().getId(),
                    interview.getPanel() == null
                            ? null
                            : interview.getPanel().getId()
            );
        }
    }
}
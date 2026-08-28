package com.mirailabs.scheduler.replan;

import com.mirailabs.scheduler.entity.*;
import com.mirailabs.scheduler.repository.*;
import com.mirailabs.scheduler.schedule.ConstraintChecker;
import com.mirailabs.scheduler.schedule.SchedulingCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ReplanService {

    private static final int SLOT_GRANULARITY_MINUTES = 15;

    private final InterviewRepository interviewRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final CompanySlotRepository companySlotRepository;
    private final ReplanAuditRepository replanAuditRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;

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

        List<ReplanAudit> audits =
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

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(interview.getId())
                                .disruptionType(
                                        ReplanDisruptionType.ROOM_UNAVAILABLE
                                )
                                .replannedAt(LocalDateTime.now())

                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())

                                .newDate(null)
                                .newStartTime(null)
                                .newEndTime(null)
                                .newRoomId(null)
                                .newPanelId(null)

                                .moved(false)
                                .cancelled(true)

                                .reason(
                                        "ROOM_UNAVAILABLE: no feasible replacement slot"
                                )
                                .build()
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

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(interview.getId())
                                .disruptionType(
                                        ReplanDisruptionType.ROOM_UNAVAILABLE
                                )
                                .replannedAt(LocalDateTime.now())

                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())

                                .newDate(candidate.date())
                                .newStartTime(candidate.startTime())
                                .newEndTime(candidate.endTime())
                                .newRoomId(candidate.room().getId())
                                .newPanelId(candidate.panel().getId())

                                .moved(true)
                                .cancelled(false)

                                .reason("ROOM_UNAVAILABLE")
                                .build()
                );
            }
        }

        interviewRepository.saveAll(affected);

        replanAuditRepository.saveAll(audits);

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

    @Transactional(readOnly = true)
    public List<Interview> getScheduledInterviewsForRoom(
            Long roomId,
            LocalDate date) {

        roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room not found: " + roomId
                        )
                );

        return interviewRepository
                .findByStatus(InterviewStatus.SCHEDULED)
                .stream()
                .filter(interview ->
                        interview.getRoom() != null)
                .filter(interview ->
                        interview.getRoom()
                                .getId()
                                .equals(roomId))
                .filter(interview ->
                        date.equals(interview.getDate()))
                .sorted(
                        Comparator
                                .comparing(Interview::getStartTime)
                                .thenComparing(Interview::getId)
                )
                .toList();
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

    @Transactional
    public PanelDropoutResult replanPanelDropout(
            PanelDropoutRequest request) {

        Panel droppedPanel =
                panelRepository.findById(request.panelId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Panel not found: "
                                                + request.panelId()
                                )
                        );

        List<Interview> scheduled =
                interviewRepository.findByStatus(
                        InterviewStatus.SCHEDULED
                );

        /*
         * Only interviews using the dropped panel
         * on the affected date are impacted.
         */
        List<Interview> affected =
                scheduled.stream()
                        .filter(interview ->
                                interview.getPanel() != null)
                        .filter(interview ->
                                interview.getPanel()
                                        .getId()
                                        .equals(
                                                request.panelId()
                                        ))
                        .filter(interview ->
                                interview.getDate()
                                        .equals(request.date()))
                        .toList();

        if (affected.isEmpty()) {

            return new PanelDropoutResult(
                    droppedPanel.getId(),
                    droppedPanel.getPanelCode(),
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        /*
         * Freeze every unaffected appointment.
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
         * Working schedule contains:
         *
         * 1. All unaffected interviews
         * 2. Successfully replanned affected interviews
         */
        List<Interview> workingSchedule =
                new ArrayList<>(frozen);

        List<ReplanChange> changes =
                new ArrayList<>();

        List<ReplanAudit> audits =
                new ArrayList<>();

        int moved = 0;
        int unscheduled = 0;

        /*
         * Deterministic processing:
         * earliest affected appointment first.
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
             * Find replacement assignment.
             *
             * The dropped panel is explicitly excluded.
             */
            SchedulingCandidate candidate =
                    findLeastDisruptivePanelCandidate(
                            interview,
                            request,
                            workingSchedule
                    );

            if (candidate == null) {

                /*
                 * No feasible replacement exists.
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
                        "PANEL_DROPOUT: no feasible replacement slot"
                );

                unscheduled++;

                changes.add(
                        buildChange(
                                interview,
                                old,
                                null,
                                "PANEL_DROPOUT"
                        )
                );

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(interview.getId())
                                .disruptionType(
                                        ReplanDisruptionType.PANEL_DROPOUT
                                )
                                .replannedAt(
                                        java.time.LocalDateTime.now()
                                )

                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())

                                .newDate(null)
                                .newStartTime(null)
                                .newEndTime(null)
                                .newRoomId(null)
                                .newPanelId(null)

                                .moved(false)
                                .cancelled(true)

                                .reason(
                                        "PANEL_DROPOUT: no feasible replacement slot"
                                )
                                .build()
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
                                "PANEL_DROPOUT"
                        )
                );

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(interview.getId())
                                .disruptionType(
                                        ReplanDisruptionType.PANEL_DROPOUT
                                )
                                .replannedAt(
                                        java.time.LocalDateTime.now()
                                )

                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())

                                .newDate(candidate.date())
                                .newStartTime(candidate.startTime())
                                .newEndTime(candidate.endTime())
                                .newRoomId(
                                        candidate.room().getId()
                                )
                                .newPanelId(
                                        candidate.panel().getId()
                                )

                                .moved(true)
                                .cancelled(false)

                                .reason("PANEL_DROPOUT")
                                .build()
                );
            }
        }

        interviewRepository.saveAll(affected);

        replanAuditRepository.saveAll(audits);

        return new PanelDropoutResult(
                droppedPanel.getId(),
                droppedPanel.getPanelCode(),
                affected.size(),
                moved,
                unscheduled,
                0,
                changes
        );
    }

    private SchedulingCandidate findLeastDisruptivePanelCandidate(
            Interview interview,
            PanelDropoutRequest request,
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
                        )
                        .stream()
                        .filter(panel ->
                                !panel.getId()
                                        .equals(request.panelId()))
                        .toList();

        List<Room> rooms =
                roomRepository.findByActiveTrue();

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
                 * Student cannot be double-booked.
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

        /*
         * Choose the least disruptive replacement.
         */
        return candidates.stream()
                .min(
                        Comparator
                                .<SchedulingCandidate>comparingInt(
                                        candidate ->
                                                disruptionCost(
                                                        interview,
                                                        candidate
                                                )
                                )
                                .thenComparing(
                                        SchedulingCandidate::date
                                )
                                .thenComparing(
                                        SchedulingCandidate::startTime
                                )
                )
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ConflictPreviewItem> getRoomImpact(
            Long roomId,
            LocalDate date) {

        roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Room not found: " + roomId
                        )
                );

        List<Interview> interviews =
                interviewRepository.findByRoomIdAndDateAndStatus(
                        roomId,
                        date,
                        InterviewStatus.SCHEDULED
                );

        return interviews.stream()
                .sorted(
                        Comparator
                                .comparing(Interview::getStartTime)
                                .thenComparing(Interview::getId)
                )
                .map(this::toConflictPreviewItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConflictPreviewItem> getPanelImpact(
            Long panelId,
            LocalDate date) {

        panelRepository.findById(panelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Panel not found: " + panelId
                        )
                );

        List<Interview> interviews =
                interviewRepository.findByPanelIdAndDateAndStatus(
                        panelId,
                        date,
                        InterviewStatus.SCHEDULED
                );

        return interviews.stream()
                .sorted(
                        Comparator
                                .comparing(Interview::getStartTime)
                                .thenComparing(Interview::getId)
                )
                .map(this::toConflictPreviewItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConflictPreviewItem> getCompanyImpact(
            Long companyId,
            LocalDate date) {

        companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Company not found: " + companyId
                        )
                );

        List<Interview> interviews =
                interviewRepository.findByCompanyIdAndDateAndStatus(
                        companyId,
                        date,
                        InterviewStatus.SCHEDULED
                );

        return interviews.stream()
                .sorted(
                        Comparator
                                .comparing(Interview::getStartTime)
                                .thenComparing(Interview::getId)
                )
                .map(this::toConflictPreviewItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConflictPreviewItem> getStudentImpact(
            Long studentId) {

        studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found: " + studentId
                        )
                );

        List<Interview> interviews =
                interviewRepository.findByStudentIdAndStatus(
                        studentId,
                        InterviewStatus.SCHEDULED
                );

        return interviews.stream()
                .sorted(
                        Comparator
                                .comparing(Interview::getDate)
                                .thenComparing(Interview::getStartTime)
                                .thenComparing(Interview::getId)
                )
                .map(this::toConflictPreviewItem)
                .toList();
    }

    private ConflictPreviewItem toConflictPreviewItem(
            Interview interview) {

        return new ConflictPreviewItem(

                interview.getId(),

                interview.getStudent().getId(),
                interview.getStudent().getStudentCode(),
                interview.getStudent().getName(),

                interview.getCompany().getId(),
                interview.getCompany().getCompanyCode(),
                interview.getCompany().getName(),

                interview.getDate(),
                interview.getStartTime(),
                interview.getEndTime(),

                interview.getRoom() == null
                        ? null
                        : interview.getRoom().getId(),

                interview.getRoom() == null
                        ? null
                        : interview.getRoom().getRoomCode(),

                interview.getPanel() == null
                        ? null
                        : interview.getPanel().getId(),

                interview.getPanel() == null
                        ? null
                        : interview.getPanel().getPanelCode()
        );
    }

    private boolean isSameAssignment(
            ReplanSnapshot old,
            SchedulingCandidate candidate) {

        if (candidate == null) {
            return false;
        }

        return Objects.equals(
                old.date(),
                candidate.date()
        )
                && Objects.equals(
                old.startTime(),
                candidate.startTime()
        )
                && Objects.equals(
                old.endTime(),
                candidate.endTime()
        )
                && Objects.equals(
                old.roomId(),
                candidate.room().getId()
        )
                && Objects.equals(
                old.panelId(),
                candidate.panel().getId()
        );
    }

    @Transactional
    public CompanyDelayResult replanCompanyDelay(
            CompanyDelayRequest request) {

        Company company =
                companyRepository.findById(request.companyId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Company not found: "
                                                + request.companyId()
                                )
                        );

        /*
         * Find the company's active slot for the affected date.
         */
        CompanySlot slot =
                companySlotRepository.findAll()
                        .stream()
                        .filter(companySlot ->
                                Boolean.TRUE.equals(
                                        companySlot.getActive()))
                        .filter(companySlot ->
                                companySlot.getCompany()
                                        .getId()
                                        .equals(company.getId()))
                        .filter(companySlot ->
                                companySlot.getDate()
                                        .equals(request.date()))
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No active company slot found for "
                                                + company.getId()
                                                + " on "
                                                + request.date()
                                )
                        );

        LocalTime oldStartTime =
                slot.getStartTime();

        LocalTime oldEndTime =
                slot.getEndTime();

        /*
         * Delay must move the start forward.
         */
        if (!request.newStartTime()
                .isAfter(oldStartTime)) {

            throw new IllegalArgumentException(
                    "New company start time must be after "
                            + oldStartTime
            );
        }

        if (!request.newStartTime()
                .isBefore(oldEndTime)) {

            throw new IllegalArgumentException(
                    "New company start time must be before "
                            + oldEndTime
            );
        }

        /*
         * Interviews that start before the new company
         * arrival time are affected.
         */
        List<Interview> affected =
                interviewRepository.findByStatus(
                                InterviewStatus.SCHEDULED
                        )
                        .stream()
                        .filter(interview ->
                                interview.getCompany()
                                        .getId()
                                        .equals(company.getId()))
                        .filter(interview ->
                                request.date()
                                        .equals(interview.getDate()))
                        .filter(interview ->
                                interview.getStartTime()
                                        .isBefore(
                                                request.newStartTime()
                                        ))
                        .toList();

        /*
         * Update company availability.
         */
        slot.setStartTime(
                request.newStartTime()
        );

        companySlotRepository.save(slot);

        if (affected.isEmpty()) {

            return new CompanyDelayResult(
                    company.getId(),
                    company.getCompanyCode(),
                    company.getName(),
                    oldStartTime,
                    request.newStartTime(),
                    oldEndTime,
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        /*
         * Freeze unaffected interviews.
         */
        List<Interview> workingSchedule =
                interviewRepository
                        .findByStatus(
                                InterviewStatus.SCHEDULED
                        )
                        .stream()
                        .filter(interview ->
                                !affected.contains(interview))
                        .collect(
                                java.util.stream.Collectors
                                        .toCollection(
                                                ArrayList::new
                                        )
                        );

        List<ReplanChange> changes =
                new ArrayList<>();

        List<ReplanAudit> audits =
                new ArrayList<>();

        /*
         * Interviews whose database state changed.
         *
         * Initially this contains the affected interviews.
         * A displacement may add another interview.
         */
        List<Interview> changedInterviews =
                new ArrayList<>(affected);

        int moved = 0;
        int unscheduled = 0;

        /*
         * Process affected interviews deterministically.
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
             * First try a completely free slot.
             */
            SchedulingCandidate candidate =
                    findLeastDisruptiveCompanyDelayCandidate(
                            interview,
                            workingSchedule
                    );

            /*
             * If no free slot exists, try one-level displacement.
             */
            DisplacementPlan displacementPlan = null;

            if (candidate == null) {

                displacementPlan =
                        findCompanyDelayDisplacementPlan(
                                interview,
                                workingSchedule
                        );
            }

            /*
             * --------------------------------------------------
             * CASE 1: No direct slot and no displacement plan
             * --------------------------------------------------
             */
            if (candidate == null
                    && displacementPlan == null) {

                interview.setDate(null);
                interview.setStartTime(null);
                interview.setEndTime(null);
                interview.setRoom(null);
                interview.setPanel(null);

                interview.setStatus(
                        InterviewStatus.UNSCHEDULED
                );

                interview.setUnscheduledReason(
                        "COMPANY_DELAY: no feasible replacement slot"
                );

                unscheduled++;

                changes.add(
                        buildChange(
                                interview,
                                old,
                                null,
                                "COMPANY_DELAY"
                        )
                );

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(
                                        interview.getId()
                                )
                                .disruptionType(
                                        ReplanDisruptionType.COMPANY_DELAY
                                )
                                .replannedAt(
                                        LocalDateTime.now()
                                )
                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())
                                .newDate(null)
                                .newStartTime(null)
                                .newEndTime(null)
                                .newRoomId(null)
                                .newPanelId(null)
                                .moved(false)
                                .cancelled(true)
                                .reason(
                                        "COMPANY_DELAY: no feasible replacement slot"
                                )
                                .build()
                );

                continue;
            }

            /*
             * --------------------------------------------------
             * CASE 2: Direct free slot found
             * --------------------------------------------------
             */
            if (candidate != null) {

                applyCandidate(
                        interview,
                        candidate
                );

                workingSchedule.add(
                        interview
                );

                /*
                 * Count the interview only when its
                 * assignment actually changed.
                 */
                if (!isSameAssignment(
                        old,
                        candidate)) {

                    moved++;

                    changes.add(
                            buildChange(
                                    interview,
                                    old,
                                    candidate,
                                    "COMPANY_DELAY"
                            )
                    );

                    audits.add(
                            ReplanAudit.builder()
                                    .interviewId(
                                            interview.getId()
                                    )
                                    .disruptionType(
                                            ReplanDisruptionType.COMPANY_DELAY
                                    )
                                    .replannedAt(
                                            LocalDateTime.now()
                                    )
                                    .oldDate(old.date())
                                    .oldStartTime(old.startTime())
                                    .oldEndTime(old.endTime())
                                    .oldRoomId(old.roomId())
                                    .oldPanelId(old.panelId())
                                    .newDate(candidate.date())
                                    .newStartTime(candidate.startTime())
                                    .newEndTime(candidate.endTime())
                                    .newRoomId(
                                            candidate.room().getId()
                                    )
                                    .newPanelId(
                                            candidate.panel().getId()
                                    )
                                    .moved(true)
                                    .cancelled(false)
                                    .reason(
                                            "COMPANY_DELAY"
                                    )
                                    .build()
                    );
                }

                continue;
            }

            if (displacementPlan == null) {
                continue;
            }

            Interview displaced =
                    displacementPlan.displacedInterview();

            SchedulingCandidate targetCandidate =
                    displacementPlan.candidate();

            SchedulingCandidate displacedReplacement =
                    displacementPlan.displacedReplacement();

            ReplanSnapshot displacedOld =
                    ReplanSnapshot.from(displaced);

            /*
             * Move the displaced interview first.
             */
            applyCandidate(
                    displaced,
                    displacedReplacement
            );

            /*
             * Update working schedule.
             */
            workingSchedule.remove(
                    displaced
            );

            workingSchedule.add(
                    displaced
            );

            /*
             * Place the delayed interview into the
             * freed slot.
             */
            applyCandidate(
                    interview,
                    targetCandidate
            );

            workingSchedule.add(
                    interview
            );

            /*
             * Make sure the displaced interview
             * is persisted too.
             */
            if (!changedInterviews.contains(
                    displaced)) {

                changedInterviews.add(
                        displaced
                );
            }

            /*
             * Count displaced interview only when
             * its assignment actually changed.
             */
            if (!isSameAssignment(
                    displacedOld,
                    displacedReplacement)) {

                moved++;

                changes.add(
                        buildChange(
                                displaced,
                                displacedOld,
                                displacedReplacement,
                                "COMPANY_DELAY: DISPLACED"
                        )
                );

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(
                                        displaced.getId()
                                )
                                .disruptionType(
                                        ReplanDisruptionType.COMPANY_DELAY
                                )
                                .replannedAt(
                                        LocalDateTime.now()
                                )
                                .oldDate(
                                        displacedOld.date()
                                )
                                .oldStartTime(
                                        displacedOld.startTime()
                                )
                                .oldEndTime(
                                        displacedOld.endTime()
                                )
                                .oldRoomId(
                                        displacedOld.roomId()
                                )
                                .oldPanelId(
                                        displacedOld.panelId()
                                )
                                .newDate(
                                        displacedReplacement.date()
                                )
                                .newStartTime(
                                        displacedReplacement.startTime()
                                )
                                .newEndTime(
                                        displacedReplacement.endTime()
                                )
                                .newRoomId(
                                        displacedReplacement
                                                .room()
                                                .getId()
                                )
                                .newPanelId(
                                        displacedReplacement
                                                .panel()
                                                .getId()
                                )
                                .moved(true)
                                .cancelled(false)
                                .reason(
                                        "COMPANY_DELAY: DISPLACED"
                                )
                                .build()
                );
            }

            /*
             * Count original delayed interview only
             * when its assignment actually changed.
             */
            if (!isSameAssignment(
                    old,
                    targetCandidate)) {

                moved++;

                changes.add(
                        buildChange(
                                interview,
                                old,
                                targetCandidate,
                                "COMPANY_DELAY"
                        )
                );

                audits.add(
                        ReplanAudit.builder()
                                .interviewId(
                                        interview.getId()
                                )
                                .disruptionType(
                                        ReplanDisruptionType.COMPANY_DELAY
                                )
                                .replannedAt(
                                        LocalDateTime.now()
                                )
                                .oldDate(old.date())
                                .oldStartTime(old.startTime())
                                .oldEndTime(old.endTime())
                                .oldRoomId(old.roomId())
                                .oldPanelId(old.panelId())
                                .newDate(
                                        targetCandidate.date()
                                )
                                .newStartTime(
                                        targetCandidate.startTime()
                                )
                                .newEndTime(
                                        targetCandidate.endTime()
                                )
                                .newRoomId(
                                        targetCandidate
                                                .room()
                                                .getId()
                                )
                                .newPanelId(
                                        targetCandidate
                                                .panel()
                                                .getId()
                                )
                                .moved(true)
                                .cancelled(false)
                                .reason(
                                        "COMPANY_DELAY"
                                )
                                .build()
                );
            }
        }

        /*
         * Save every interview whose schedule changed.
         */
        interviewRepository.saveAll(
                changedInterviews
        );

        replanAuditRepository.saveAll(
                audits
        );

        return new CompanyDelayResult(
                company.getId(),
                company.getCompanyCode(),
                company.getName(),
                oldStartTime,
                request.newStartTime(),
                oldEndTime,
                affected.size(),
                moved,
                unscheduled,
                0,
                changes
        );
    }

    private DisplacementPlan findCompanyDelayDisplacementPlan(
            Interview interview,
            List<Interview> workingSchedule) {

        List<CompanySlot> activeSlots =
                companySlotRepository.findAll()
                        .stream()
                        .filter(slot ->
                                Boolean.TRUE.equals(
                                        slot.getActive()))
                        .filter(slot ->
                                slot.getCompany()
                                        .getId()
                                        .equals(
                                                interview
                                                        .getCompany()
                                                        .getId()
                                        ))
                        .toList();

        List<Panel> panels =
                panelRepository
                        .findByCompanyIdAndActiveTrue(
                                interview
                                        .getCompany()
                                        .getId()
                        );

        List<Room> rooms =
                roomRepository.findByActiveTrue();

        int duration =
                interview.getCompany()
                        .getInterviewDurationMinutes();

        for (CompanySlot slot : activeSlots) {

            LocalTime current =
                    slot.getStartTime();

            while (!current.plusMinutes(duration)
                    .isAfter(slot.getEndTime())) {

                LocalTime end =
                        current.plusMinutes(duration);

                /*
                 * The affected student's availability
                 * is still a hard constraint.
                 */
                if (constraintChecker.hasStudentConflict(
                        interview,
                        slot.getDate(),
                        current,
                        end,
                        workingSchedule)) {

                    current = current.plusMinutes(
                            SLOT_GRANULARITY_MINUTES
                    );

                    continue;
                }

                for (Panel panel : panels) {

                    final LocalDate candidateDate = slot.getDate();
                    final LocalTime candidateStart = current;
                    final LocalTime candidateEnd = end;

                    List<Interview> panelConflicts =
                            workingSchedule.stream()
                                    .filter(existing ->
                                            existing.getPanel() != null)
                                    .filter(existing ->
                                            existing.getPanel()
                                                    .getId()
                                                    .equals(panel.getId()))
                                    .filter(existing ->
                                            overlaps(
                                                    existing,
                                                    candidateDate,
                                                    candidateStart,
                                                    candidateEnd))
                                    .toList();

                    /*
                     * More than one panel conflict would require
                     * more than one displacement.
                     */
                    if (panelConflicts.size() > 1) {
                        continue;
                    }

                    for (Room room : rooms) {

                        List<Interview> roomConflicts =
                                workingSchedule.stream()
                                        .filter(existing ->
                                                existing.getRoom() != null)
                                        .filter(existing ->
                                                existing.getRoom()
                                                        .getId()
                                                        .equals(room.getId()))
                                        .filter(existing ->
                                                overlaps(
                                                        existing,
                                                        candidateDate,
                                                        candidateStart,
                                                        candidateEnd))
                                        .toList();

                        /*
                         * More than one room conflict would require
                         * multiple displacements.
                         */
                        if (roomConflicts.size() > 1) {
                            continue;
                        }

                        /*
                         * Collect the appointments occupying
                         * the target panel/room.
                         */
                        java.util.Set<Interview> conflicts =
                                new java.util.LinkedHashSet<>();

                        conflicts.addAll(
                                panelConflicts
                        );

                        conflicts.addAll(
                                roomConflicts
                        );

                        /*
                         * A direct free candidate should already
                         * have been handled by the normal finder.
                         */
                        if (conflicts.isEmpty()) {
                            continue;
                        }

                        /*
                         * We support exactly one displaced
                         * appointment.
                         */
                        if (conflicts.size() != 1) {
                            continue;
                        }

                        Interview displaced =
                                conflicts.iterator().next();

                        SchedulingCandidate targetCandidate =
                                new SchedulingCandidate(
                                        slot.getDate(),
                                        current,
                                        end,
                                        room,
                                        panel
                                );

                        /*
                         * Remove the displaced appointment
                         * temporarily.
                         */
                        List<Interview> replacementSchedule =
                                new ArrayList<>(
                                        workingSchedule
                                );

                        replacementSchedule.remove(
                                displaced
                        );

                        /*
                         * Reserve the target slot for the
                         * delayed interview.
                         */
                        replacementSchedule.add(
                                interview
                        );

                        /*
                         * Find somewhere else for the
                         * displaced appointment.
                         */
                        SchedulingCandidate replacement =
                                findLeastDisruptiveCompanyDelayCandidate(
                                        displaced,
                                        replacementSchedule
                                );

                        if (replacement != null) {

                            return new DisplacementPlan(
                                    targetCandidate,
                                    displaced,
                                    replacement
                            );
                        }
                    }
                }

                current = current.plusMinutes(
                        SLOT_GRANULARITY_MINUTES
                );
            }
        }

        return null;
    }

    private boolean overlaps(
            Interview interview,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {

        if (interview.getDate() == null
                || interview.getStartTime() == null
                || interview.getEndTime() == null) {

            return false;
        }

        if (!interview.getDate().equals(date)) {
            return false;
        }

        return interview.getStartTime().isBefore(endTime)
                && interview.getEndTime().isAfter(startTime);
    }

    private SchedulingCandidate findLeastDisruptiveCompanyDelayCandidate(
            Interview interview,
            List<Interview> workingSchedule) {

        int totalTimeSlots = 0;
        int studentConflictCount = 0;
        int panelConflictCount = 0;
        int roomConflictCount = 0;
        int feasibleCount = 0;

        List<CompanySlot> activeSlots =
                companySlotRepository.findAll()
                        .stream()
                        .filter(slot ->
                                Boolean.TRUE.equals(
                                        slot.getActive()))
                        .filter(slot ->
                                slot.getCompany()
                                        .getId()
                                        .equals(
                                                interview
                                                        .getCompany()
                                                        .getId()
                                        ))
                        .filter(slot ->
                                slot.getDate()
                                        .equals(interview.getDate())
                        )
                        .toList();

        List<Panel> panels =
                panelRepository
                        .findByCompanyIdAndActiveTrue(
                                interview
                                        .getCompany()
                                        .getId()
                        );

        List<Room> rooms =
                roomRepository.findByActiveTrue();

        List<SchedulingCandidate> candidates =
                new ArrayList<>();

        int duration =
                interview.getCompany()
                        .getInterviewDurationMinutes();

        for (CompanySlot slot : activeSlots) {

            LocalTime current = slot.getStartTime();

            while (!current.plusMinutes(duration)
                    .isAfter(slot.getEndTime())) {

                totalTimeSlots++;

                LocalTime end =
                        current.plusMinutes(duration);

                /*
                 * Student conflict
                 */
                if (constraintChecker.hasStudentConflict(
                        interview,
                        slot.getDate(),
                        current,
                        end,
                        workingSchedule)) {

                    studentConflictCount++;

                    current = current.plusMinutes(
                            SLOT_GRANULARITY_MINUTES
                    );

                    continue;
                }

                for (Panel panel : panels) {

                    if (constraintChecker.hasPanelConflict(
                            panel,
                            slot.getDate(),
                            current,
                            end,
                            workingSchedule)) {

                        panelConflictCount++;
                        continue;
                    }

                    for (Room room : rooms) {

                        if (constraintChecker.hasRoomConflict(
                                room,
                                slot.getDate(),
                                current,
                                end,
                                workingSchedule)) {

                            roomConflictCount++;
                            continue;
                        }

                        feasibleCount++;

                        candidates.add(
                                new SchedulingCandidate(
                                        slot.getDate(),
                                        current,
                                        end,
                                        room,
                                        panel
                                )
                        );
                    }
                }

                current = current.plusMinutes(
                        SLOT_GRANULARITY_MINUTES
                );
            }
        }

        System.out.println(
                "COMPANY_DELAY candidate count for interview "
                        + interview.getId()
                        + " = "
                        + candidates.size()
        );

        System.out.println(
                "COMPANY_DELAY DEBUG | interview="
                        + interview.getId()
                        + " | slots="
                        + activeSlots.size()
                        + " | panels="
                        + panels.size()
                        + " | rooms="
                        + rooms.size()
                        + " | timeSlots="
                        + totalTimeSlots
                        + " | studentConflicts="
                        + studentConflictCount
                        + " | panelConflicts="
                        + panelConflictCount
                        + " | roomConflicts="
                        + roomConflictCount
                        + " | feasible="
                        + feasibleCount
        );

        return candidates.stream()
                .min(
                        Comparator
                                .<SchedulingCandidate>comparingInt(
                                        candidate ->
                                                disruptionCost(
                                                        interview,
                                                        candidate
                                                )
                                )
                                .thenComparing(
                                        SchedulingCandidate::date
                                )
                                .thenComparing(
                                        SchedulingCandidate::startTime
                                )
                )
                .orElse(null);
    }

    private record DisplacementPlan(
            SchedulingCandidate candidate,
            Interview displacedInterview,
            SchedulingCandidate displacedReplacement
    ) {
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

    @Transactional
    public StudentWithdrawResult replanStudentWithdraw(
            StudentWithdrawRequest request) {

        /*
         * Find the student.
         */
        Student student =
                studentRepository.findById(request.studentId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Student not found: "
                                                + request.studentId()
                                )
                        );

        /*
         * A student who is already withdrawn does not
         * need to be withdrawn again.
         */
        if (student.getStatus() == StudentStatus.WITHDRAWN) {

            return new StudentWithdrawResult(
                    student.getId(),
                    student.getStudentCode(),
                    student.getName(),
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        /*
         * Change the student's status first.
         */
        student.setStatus(
                StudentStatus.WITHDRAWN
        );

        /*
         * Only currently scheduled interviews are affected.
         *
         * We intentionally do NOT touch:
         *
         * - already UNSCHEDULED interviews
         * - other students
         * - other companies
         */
        List<Interview> affected =
                interviewRepository.findByStudentId(
                                student.getId()
                        )
                        .stream()
                        .filter(interview ->
                                interview.getStatus()
                                        == InterviewStatus.SCHEDULED)
                        .toList();

        List<ReplanChange> changes =
                new ArrayList<>();

        List<ReplanAudit> audits =
                new ArrayList<>();

        /*
         * Cancel every scheduled interview belonging
         * to this student.
         */
        for (Interview interview : affected) {

            ReplanSnapshot old =
                    ReplanSnapshot.from(interview);

            /*
             * Remove the schedule assignment.
             */
            interview.setDate(null);
            interview.setStartTime(null);
            interview.setEndTime(null);
            interview.setRoom(null);
            interview.setPanel(null);

            interview.setStatus(
                    InterviewStatus.UNSCHEDULED
            );

            interview.setUnscheduledReason(
                    "STUDENT_WITHDRAWN"
            );

            /*
             * Build API change response.
             */
            changes.add(
                    buildChange(
                            interview,
                            old,
                            null,
                            "STUDENT_WITHDRAWN"
                    )
            );

            /*
             * Create audit entry.
             */
            audits.add(
                    ReplanAudit.builder()
                            .interviewId(
                                    interview.getId()
                            )
                            .disruptionType(
                                    ReplanDisruptionType.STUDENT_WITHDRAWN
                            )
                            .replannedAt(
                                    LocalDateTime.now()
                            )

                            .oldDate(
                                    old.date()
                            )
                            .oldStartTime(
                                    old.startTime()
                            )
                            .oldEndTime(
                                    old.endTime()
                            )
                            .oldRoomId(
                                    old.roomId()
                            )
                            .oldPanelId(
                                    old.panelId()
                            )

                            .newDate(null)
                            .newStartTime(null)
                            .newEndTime(null)
                            .newRoomId(null)
                            .newPanelId(null)

                            .moved(false)
                            .cancelled(true)

                            .reason(
                                    "STUDENT_WITHDRAWN"
                            )
                            .build()
            );
        }

        /*
         * Persist student status.
         */
        studentRepository.save(student);

        /*
         * Persist changed interviews.
         */
        if (!affected.isEmpty()) {

            interviewRepository.saveAll(
                    affected
            );
        }

        /*
         * Persist replan audit records.
         */
        if (!audits.isEmpty()) {

            replanAuditRepository.saveAll(
                    audits
            );
        }

        return new StudentWithdrawResult(
                student.getId(),
                student.getStudentCode(),
                student.getName(),
                affected.size(),
                affected.size(),
                0,
                changes
        );
    }

    private ConflictReplanResponse toConflictResponse(
            ReplanResult result) {

        ConflictSummary summary =
                new ConflictSummary(
                        "ROOM_UNAVAILABLE",
                        result.affectedAppointments(),
                        result.movedAppointments(),
                        result.unscheduledAppointments(),
                        0,
                        result.unchangedAppointments(),
                        result.changes().size()
                );

        return new ConflictReplanResponse(
                summary,
                result.changes()
        );
    }

    private ConflictReplanResponse toConflictResponse(
            PanelDropoutResult result) {

        ConflictSummary summary =
                new ConflictSummary(
                        "PANEL_DROPOUT",
                        result.affectedAppointments(),
                        result.movedAppointments(),
                        result.unscheduledAppointments(),
                        0,
                        result.unchangedAppointments(),
                        result.changes().size()
                );

        return new ConflictReplanResponse(
                summary,
                result.changes()
        );
    }

    private ConflictReplanResponse toConflictResponse(
            CompanyDelayResult result) {

        ConflictSummary summary =
                new ConflictSummary(
                        "COMPANY_DELAY",
                        result.affectedAppointments(),
                        result.movedAppointments(),
                        result.unscheduledAppointments(),
                        0,
                        result.unchangedAppointments(),
                        result.changes().size()
                );

        return new ConflictReplanResponse(
                summary,
                result.changes()
        );
    }

    private ConflictReplanResponse toConflictResponse(
            StudentWithdrawResult result) {

        ConflictSummary summary =
                new ConflictSummary(
                        "STUDENT_WITHDRAWN",
                        result.affectedAppointments(),
                        0,
                        0,
                        result.cancelledAppointments(),
                        result.unchangedAppointments(),
                        result.changes().size()
                );

        return new ConflictReplanResponse(
                summary,
                result.changes()
        );
    }

    @Transactional
    public ConflictReplanResponse executeConflict(
            ConflictCenterRequest request) {

        return switch (request.conflictType()) {

            case "ROOM_UNAVAILABLE" -> {

                ReplanResult result =
                        replanRoomUnavailable(
                                new RoomUnavailableRequest(
                                        request.roomId(),
                                        request.date()
                                )
                        );

                yield toConflictResponse(result);
            }

            case "PANEL_DROPOUT" -> {

                PanelDropoutResult result =
                        replanPanelDropout(
                                new PanelDropoutRequest(
                                        request.panelId(),
                                        request.date()
                                )
                        );

                yield toConflictResponse(result);
            }

            case "COMPANY_DELAY" -> {

                CompanyDelayResult result =
                        replanCompanyDelay(
                                new CompanyDelayRequest(
                                        request.companyId(),
                                        request.date(),
                                        request.newStartTime()
                                )
                        );

                yield toConflictResponse(result);
            }

            case "STUDENT_WITHDRAWN" -> {

                StudentWithdrawResult result =
                        replanStudentWithdraw(
                                new StudentWithdrawRequest(
                                        request.studentId()
                                )
                        );

                yield toConflictResponse(result);
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported conflict type: "
                                    + request.conflictType()
                    );
        };
    }

    @Transactional(readOnly = true)
    public List<ReplanHistoryItem> getReplanHistory() {

        return replanAuditRepository
                .findAllByOrderByReplannedAtDesc()
                .stream()
                .map(this::toReplanHistoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplanHistoryItem> getReplanHistoryByType(
            ReplanDisruptionType disruptionType) {

        return replanAuditRepository
                .findByDisruptionTypeOrderByReplannedAtDesc(
                        disruptionType
                )
                .stream()
                .map(this::toReplanHistoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReplanHistoryItem> getReplanHistoryByDate(
            LocalDate date) {

        LocalDateTime start =
                date.atStartOfDay();

        LocalDateTime end =
                date.plusDays(1)
                        .atStartOfDay();

        return replanAuditRepository
                .findByReplannedAtBetweenOrderByReplannedAtDesc(
                        start,
                        end
                )
                .stream()
                .map(this::toReplanHistoryItem)
                .toList();
    }

    private ReplanHistoryItem toReplanHistoryItem(
            ReplanAudit audit) {

        return new ReplanHistoryItem(

                audit.getId(),

                audit.getInterviewId(),

                audit.getDisruptionType(),

                audit.getReplannedAt(),

                audit.getOldDate(),
                audit.getOldStartTime(),
                audit.getOldEndTime(),
                audit.getOldRoomId(),
                audit.getOldPanelId(),

                audit.getNewDate(),
                audit.getNewStartTime(),
                audit.getNewEndTime(),
                audit.getNewRoomId(),
                audit.getNewPanelId(),

                audit.isMoved(),
                audit.isCancelled(),

                audit.getReason()
        );
    }





}
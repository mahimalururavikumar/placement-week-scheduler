package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.schedule.ScheduleGenerationResult;
import com.mirailabs.scheduler.schedule.ScheduleItem;
import com.mirailabs.scheduler.schedule.ScheduleService;
import com.mirailabs.scheduler.schedule.ScheduleSummary;
import com.mirailabs.scheduler.schedule.SchedulingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final ScheduleService scheduleService;

    private final SchedulingEngine schedulingEngine;

    /*
     * --------------------------------------------------
     * GENERATE INITIAL SCHEDULE
     *
     * POST /api/scheduling/generate
     *
     * This calls the actual SchedulingEngine and returns
     * the generation result.
     * --------------------------------------------------
     */
    @PostMapping("/generate")
    public ResponseEntity<ScheduleGenerationResult> generateSchedule() {

        ScheduleGenerationResult result =
                schedulingEngine.generateInitialSchedule();

        return ResponseEntity.ok(result);
    }

    /*
     * --------------------------------------------------
     * GET COMPLETE / FILTERED SCHEDULE
     *
     * Examples:
     *
     * /api/scheduling/schedule
     * /api/scheduling/schedule?date=2026-08-26
     * /api/scheduling/schedule?companyId=34
     * /api/scheduling/schedule?roomId=7
     * /api/scheduling/schedule?panelId=94
     * /api/scheduling/schedule?studentId=481
     *
     * Multiple filters can be combined.
     * --------------------------------------------------
     */
    @GetMapping("/schedule")
    public ResponseEntity<List<ScheduleItem>> getSchedule(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date,

            @RequestParam(required = false)
            Long companyId,

            @RequestParam(required = false)
            Long studentId,

            @RequestParam(required = false)
            Long roomId,

            @RequestParam(required = false)
            Long panelId) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        date,
                        companyId,
                        studentId,
                        roomId,
                        panelId
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE BY DATE
     * --------------------------------------------------
     */
    @GetMapping("/schedule/date/{date}")
    public ResponseEntity<List<ScheduleItem>> getScheduleByDate(

            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        date,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE BY COMPANY
     * --------------------------------------------------
     */
    @GetMapping("/schedule/company/{companyId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleByCompany(
            @PathVariable Long companyId) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        null,
                        companyId,
                        null,
                        null,
                        null
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE BY STUDENT
     * --------------------------------------------------
     */
    @GetMapping("/schedule/student/{studentId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleByStudent(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        null,
                        null,
                        studentId,
                        null,
                        null
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE BY ROOM
     * --------------------------------------------------
     */
    @GetMapping("/schedule/room/{roomId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleByRoom(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        null,
                        null,
                        null,
                        roomId,
                        null
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE BY PANEL
     * --------------------------------------------------
     */
    @GetMapping("/schedule/panel/{panelId}")
    public ResponseEntity<List<ScheduleItem>> getScheduleByPanel(
            @PathVariable Long panelId) {

        return ResponseEntity.ok(
                scheduleService.getSchedule(
                        null,
                        null,
                        null,
                        null,
                        panelId
                )
        );
    }

    /*
     * --------------------------------------------------
     * SCHEDULE SUMMARY
     * --------------------------------------------------
     */
    @GetMapping("/schedule/summary")
    public ResponseEntity<ScheduleSummary> getScheduleSummary() {

        return ResponseEntity.ok(
                scheduleService.getScheduleSummary()
        );
    }
}
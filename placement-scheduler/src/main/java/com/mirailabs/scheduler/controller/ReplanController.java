package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.replan.*;
import com.mirailabs.scheduler.entity.ReplanDisruptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/replan")
@RequiredArgsConstructor
public class ReplanController {

    private final ReplanService replanService;

    @GetMapping("/room/{roomId}/scheduled")
    public ResponseEntity<List<ConflictPreviewItem>> roomImpact(
            @PathVariable Long roomId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                replanService.getRoomImpact(
                        roomId,
                        date
                )
        );
    }

    @PostMapping("/room-unavailable")
    public ResponseEntity<ReplanResult> roomUnavailable(
            @RequestBody RoomUnavailableRequest request) {

        return ResponseEntity.ok(
                replanService.replanRoomUnavailable(
                        request
                )
        );
    }

    @PostMapping("/panel-drop")
    public ResponseEntity<PanelDropoutResult> panelDropout(
            @RequestBody PanelDropoutRequest request) {

        return ResponseEntity.ok(
                replanService.replanPanelDropout(
                        request
                )
        );
    }

    @PostMapping("/company-delay")
    public ResponseEntity<CompanyDelayResult> companyDelay(
            @RequestBody CompanyDelayRequest request) {

        return ResponseEntity.ok(
                replanService.replanCompanyDelay(request)
        );
    }

    @PostMapping("/student-withdraw")
    public ResponseEntity<StudentWithdrawResult> studentWithdraw(
            @RequestBody StudentWithdrawRequest request) {

        return ResponseEntity.ok(
                replanService.replanStudentWithdraw(
                        request
                )
        );
    }

    @GetMapping("/panel/{panelId}/scheduled")
    public ResponseEntity<List<ConflictPreviewItem>> panelImpact(
            @PathVariable Long panelId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                replanService.getPanelImpact(
                        panelId,
                        date
                )
        );
    }

    @GetMapping("/company/{companyId}/scheduled")
    public ResponseEntity<List<ConflictPreviewItem>> companyImpact(
            @PathVariable Long companyId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                replanService.getCompanyImpact(
                        companyId,
                        date
                )
        );
    }

    @GetMapping("/student/{studentId}/scheduled")
    public ResponseEntity<List<ConflictPreviewItem>> studentImpact(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                replanService.getStudentImpact(
                        studentId
                )
        );
    }

    @PostMapping("/conflict")
    public ResponseEntity<ConflictReplanResponse> executeConflict(
            @RequestBody ConflictCenterRequest request) {

        return ResponseEntity.ok(
                replanService.executeConflict(request)
        );
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReplanHistoryItem>>
    getReplanHistory() {

        return ResponseEntity.ok(
                replanService.getReplanHistory()
        );
    }

    @GetMapping("/history/type/{type}")
    public ResponseEntity<List<ReplanHistoryItem>>
    getReplanHistoryByType(
            @PathVariable ReplanDisruptionType type) {

        return ResponseEntity.ok(
                replanService.getReplanHistoryByType(
                        type
                )
        );
    }

    @GetMapping("/history/date/{date}")
    public ResponseEntity<List<ReplanHistoryItem>>
    getReplanHistoryByDate(

            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date) {

        return ResponseEntity.ok(
                replanService.getReplanHistoryByDate(
                        date
                )
        );
    }




}
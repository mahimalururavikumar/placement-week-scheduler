package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.replan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/replan")
@RequiredArgsConstructor
public class ReplanController {

    private final ReplanService replanService;

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
}
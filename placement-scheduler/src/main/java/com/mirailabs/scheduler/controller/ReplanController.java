package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.replan.ReplanResult;
import com.mirailabs.scheduler.replan.ReplanService;
import com.mirailabs.scheduler.replan.RoomUnavailableRequest;
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
}
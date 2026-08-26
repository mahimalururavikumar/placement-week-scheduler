package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.scheduler.SchedulingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingEngine schedulingEngine;

    @PostMapping("/generate")
    public String generateSchedule() {

        schedulingEngine.generateInitialSchedule();

        return "Initial schedule generated successfully.";
    }
}
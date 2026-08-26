package com.mirailabs.scheduler.controller;

import com.mirailabs.scheduler.metrics.MetricsService;
import com.mirailabs.scheduler.metrics.MetricsSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/summary")
    public MetricsSummary getSummary() {
        return metricsService.calculateSummary();
    }
}
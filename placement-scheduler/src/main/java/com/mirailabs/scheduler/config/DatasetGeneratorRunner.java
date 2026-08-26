package com.mirailabs.scheduler.config;

import com.mirailabs.scheduler.service.DatasetGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatasetGeneratorRunner implements CommandLineRunner {

    private final DatasetGeneratorService datasetGeneratorService;

    @Override
    public void run(String... args) {
        datasetGeneratorService.generateDataset();
    }
}
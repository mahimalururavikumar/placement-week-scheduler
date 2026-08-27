package com.mirailabs.scheduler.replan;

import java.time.LocalDate;

public record PanelDropoutRequest(
        Long panelId,
        LocalDate date
) {
}
package com.mirailabs.scheduler.replan;

import java.time.LocalDate;

public record RoomUnavailableRequest(
        Long roomId,
        LocalDate date
) {
}

package com.mirailabs.scheduler.replan;

import java.time.LocalDate;
import java.time.LocalTime;

public record CompanyDelayRequest(

        Long companyId,

        LocalDate date,

        LocalTime newStartTime
) {
}
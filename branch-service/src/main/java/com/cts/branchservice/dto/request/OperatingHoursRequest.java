package com.cts.branchservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingHoursRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    // null = closed; required when isClosed = false
    private LocalTime openTime;
    private LocalTime closeTime;

    @NotNull(message = "Please specify whether the branch is closed on this day")
    private Boolean isClosed;
}

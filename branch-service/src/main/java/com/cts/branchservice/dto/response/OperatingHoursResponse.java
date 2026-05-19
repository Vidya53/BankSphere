package com.cts.branchservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingHoursResponse {

    private DayOfWeek dayOfWeek;
    private Boolean isClosed;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;
}

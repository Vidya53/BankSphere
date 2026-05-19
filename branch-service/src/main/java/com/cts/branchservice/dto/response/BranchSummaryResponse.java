package com.cts.branchservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchSummaryResponse {

    private BranchResponse branch;

    private long totalEmployees;
    private long activeEmployees;
    private Map<String, Long> employeesByDesignation;

    private String branchManagerName;
    private boolean isCurrentlyOpen;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime todayOpenTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime todayCloseTime;

    private List<OperatingHoursResponse> weeklySchedule;
}

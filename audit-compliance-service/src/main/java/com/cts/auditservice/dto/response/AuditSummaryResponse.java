package com.cts.auditservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Builder
public class AuditSummaryResponse {

    private long totalEvents;
    private long successEvents;
    private long failureEvents;
    private long pendingEvents;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;

    // Number of events per service (e.g. "account-service" → 342)
    private Map<String, Long> eventsByService;

    // Number of events per action (e.g. "ACCOUNT_FROZEN" → 15)
    private Map<String, Long> eventsByAction;

    // Number of events per calendar day (e.g. "2024-06-01" → 58)
    private Map<String, Long> eventsByDay;

    // Top 10 most active users
    private Map<String, Long> topPerformers;
}

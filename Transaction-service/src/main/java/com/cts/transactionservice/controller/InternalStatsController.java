package com.cts.transactionservice.controller;

import com.cts.transactionservice.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal aggregations consumed by audit-compliance-service's Analytics
 * dashboard. Not routed through the gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/stats")
@RequiredArgsConstructor
@Tag(name = "Internal · Transaction Stats", description = "Internal Feign-only transaction aggregations consumed by audit-compliance-service")
public class InternalStatsController {

    private final TransactionRepository transactionRepository;

    @Operation(
            summary = "Aggregate transaction statistics for analytics",
            description = """
                    Returns a portfolio-wide rollup: total count and volume, status/type/channel breakdowns, daily volume for the last 30 days, and monthly volume by type for the last 12 months. Powers the audit-compliance Analytics dashboard.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> transactionStats() {
        long total = transactionRepository.count();
        BigDecimal totalVolume = transactionRepository.totalSuccessfulVolume();
        if (totalVolume == null) totalVolume = BigDecimal.ZERO;

        // Status breakdown
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : transactionRepository.countGroupByStatus()) {
            if (row[0] == null) continue;
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        // Type breakdown
        List<Map<String, Object>> byType = new ArrayList<>();
        for (Object[] row : transactionRepository.volumeGroupByTransactionType()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",   row[0]);
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2] != null ? row[2] : BigDecimal.ZERO);
            byType.add(entry);
        }

        // Channel breakdown — last 30 days
        LocalDateTime since30 = LocalDate.now().minusDays(30).atStartOfDay();
        List<Map<String, Object>> byChannel = new ArrayList<>();
        for (Object[] row : transactionRepository.volumeGroupByChannelSince(since30)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",   row[0] != null ? row[0].toString() : "OTHER");
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2] != null ? row[2] : BigDecimal.ZERO);
            byChannel.add(entry);
        }

        // Daily volume — last 30 days
        List<Map<String, Object>> dailyVolume = new ArrayList<>();
        for (Object[] row : transactionRepository.dailySuccessVolume(since30, LocalDateTime.now())) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date",   row[0] != null ? row[0].toString() : null);
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2] != null ? row[2] : BigDecimal.ZERO);
            dailyVolume.add(entry);
        }

        // Monthly volume by type — last 12 months
        LocalDateTime since12mo = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<Map<String, Object>> monthlyByType = new ArrayList<>();
        for (Object[] row : transactionRepository.monthlySuccessVolumeByType(since12mo)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("year",   ((Number) row[0]).intValue());
            entry.put("month",  ((Number) row[1]).intValue());
            entry.put("type",   row[2] != null ? row[2].toString() : "OTHER");
            entry.put("count",  ((Number) row[3]).longValue());
            entry.put("amount", row[4] != null ? row[4] : BigDecimal.ZERO);
            monthlyByType.add(entry);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCount",     total);
        out.put("totalVolume",    totalVolume);
        out.put("byStatus",       byStatus);
        out.put("byType",         byType);
        out.put("byChannel30d",   byChannel);
        out.put("dailyVolume30d", dailyVolume);
        out.put("monthlyByType",  monthlyByType);
        return ResponseEntity.ok(out);
    }
}

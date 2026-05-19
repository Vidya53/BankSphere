package com.cts.customerservices.controller;

import com.cts.customerservices.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal stats endpoint consumed by audit-compliance-service for the
 * analytics dashboard. Not routed by the API gateway — only reachable
 * from other services within the cluster.
 */
@RestController
@RequestMapping("/api/v1/internal/stats")
@RequiredArgsConstructor
@Tag(name = "Internal · Stats", description = "Internal-only customer analytics aggregations consumed by audit-compliance-service")
public class InternalStatsController {

    private final CustomerRepository customerRepository;

    @GetMapping("/customers")
    @Operation(
        summary = "Aggregated customer analytics for the audit-compliance dashboard",
        description = """
                Returns total active customers, today + month-to-date acquisition counts, breakdowns by
                status and risk band, top 6 cities by customer count, and a 12-month acquisition trend.

                **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<Map<String, Object>> customerStats() {
        long total = customerRepository.countAllActive();

        // Status breakdown — keep ordered, drop nulls
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : customerRepository.countByStatusGrouped()) {
            if (row[0] == null) continue;
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        Map<String, Long> byRisk = new LinkedHashMap<>();
        for (Object[] row : customerRepository.countByRiskGrouped()) {
            if (row[0] == null) continue;
            byRisk.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        // Top 6 cities
        List<Map<String, Object>> topCities = new ArrayList<>();
        for (Object[] row : customerRepository.countByCityGrouped(PageRequest.of(0, 6))) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",  row[0]);
            entry.put("value", ((Number) row[1]).longValue());
            topCities.add(entry);
        }

        // Acquisition by month for the last 12 months
        LocalDateTime since = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<Map<String, Object>> acquisition = new ArrayList<>();
        for (Object[] row : customerRepository.acquisitionByMonth(since)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            entry.put("year",         year);
            entry.put("month",        month);
            entry.put("monthLabel",   monthLabel(month) + " " + year);
            entry.put("newCustomers", ((Number) row[2]).longValue());
            acquisition.add(entry);
        }

        // Today + active-30d counts
        long newToday    = customerRepository.findByCreatedAtBetween(
                LocalDate.now().atStartOfDay(), LocalDateTime.now()).size();
        long newThisMonth = customerRepository.findByCreatedAtBetween(
                LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDateTime.now()).size();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total",         total);
        out.put("newToday",      newToday);
        out.put("newThisMonth",  newThisMonth);
        out.put("byStatus",      byStatus);
        out.put("byRisk",        byRisk);
        out.put("topCities",     topCities);
        out.put("acquisition",   acquisition);
        return ResponseEntity.ok(out);
    }

    private static String monthLabel(int month) {
        return switch (month) {
            case 1 -> "Jan"; case 2 -> "Feb"; case 3 -> "Mar";  case 4 -> "Apr";
            case 5 -> "May"; case 6 -> "Jun"; case 7 -> "Jul";  case 8 -> "Aug";
            case 9 -> "Sep"; case 10 -> "Oct"; case 11 -> "Nov"; case 12 -> "Dec";
            default -> "?";
        };
    }
}

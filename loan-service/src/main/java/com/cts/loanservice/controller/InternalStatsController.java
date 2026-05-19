package com.cts.loanservice.controller;

import com.cts.loanservice.repository.LoanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal loan-portfolio aggregations consumed by audit-compliance-service's
 * Analytics dashboard. Not routed through the gateway.
 */
@RestController
@RequestMapping("/api/v1/internal/stats")
@RequiredArgsConstructor
@Tag(name = "Internal · Loan Stats", description = "Internal Feign-only loan-portfolio aggregations consumed by audit-compliance-service and branch-manager dashboards")
public class InternalStatsController {

    private final LoanRepository loanRepository;

    @Operation(
            summary = "Month-to-date loan KPIs",
            description = """
                    Returns this month's application count, approved (+ disbursed) count, and total disbursed amount. Consumed by the branch-manager dashboard for its KPI cards.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/loans/mtd")
    public ResponseEntity<Map<String, Object>> loansMtd() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime monthFrom = today.withDayOfMonth(1).atStartOfDay();
        java.time.LocalDateTime monthTo   = today.plusDays(1).atStartOfDay();

        long applications  = loanRepository.countCreatedBetween(monthFrom, monthTo, null);
        long approvedMtd   = loanRepository.countCreatedBetween(monthFrom, monthTo, com.cts.loanservice.entity.LoanStatus.APPROVED)
                           + loanRepository.countCreatedBetween(monthFrom, monthTo, com.cts.loanservice.entity.LoanStatus.DISBURSED);
        Double disbursed   = loanRepository.sumDisbursedBetween(monthFrom, monthTo);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("applicationsThisMonth", applications);
        out.put("approvedThisMonth",     approvedMtd);
        out.put("disbursedAmountThisMonth", disbursed == null ? 0.0 : disbursed);
        return ResponseEntity.ok(out);
    }

    @Operation(
            summary = "Aggregate loan portfolio statistics",
            description = """
                    Returns portfolio-wide rollups: total/active loan counts, total outstanding portfolio amount, average ticket size, plus breakdowns by status, by loan type, and monthly disbursements.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/loans")
    public ResponseEntity<Map<String, Object>> loanStats() {
        long totalLoans = loanRepository.count();
        long activeCount = loanRepository.countActive();
        Double portfolio = nz(loanRepository.totalPortfolioAmount());
        Double avgTicket = nz(loanRepository.averageTicketSize());

        List<Map<String, Object>> byStatus = new ArrayList<>();
        for (Object[] row : loanRepository.aggregateByStatus()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",   row[0] != null ? row[0].toString() : "UNKNOWN");
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2] != null ? row[2] : 0.0);
            byStatus.add(entry);
        }

        List<Map<String, Object>> byType = new ArrayList<>();
        for (Object[] row : loanRepository.aggregateByType()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",   row[0] != null ? row[0].toString() : "UNKNOWN");
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2] != null ? row[2] : 0.0);
            byType.add(entry);
        }

        List<Map<String, Object>> monthly = new ArrayList<>();
        for (Object[] row : loanRepository.disbursementsByMonth()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("year",   ((Number) row[0]).intValue());
            entry.put("month",  ((Number) row[1]).intValue());
            entry.put("count",  ((Number) row[2]).longValue());
            entry.put("amount", row[3] != null ? row[3] : 0.0);
            monthly.add(entry);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalLoans",    totalLoans);
        out.put("activeCount",   activeCount);
        out.put("portfolio",     portfolio);
        out.put("averageTicket", avgTicket);
        out.put("byStatus",      byStatus);
        out.put("byType",        byType);
        out.put("monthly",       monthly);
        return ResponseEntity.ok(out);
    }

    private static Double nz(Double d) { return d == null ? 0.0 : d; }
}

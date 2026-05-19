package com.cts.accountservice.controller;

import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.repository.AccountApplicationRepository;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.repository.PendingTransferRepository;
import com.cts.accountservice.entity.PendingTransfer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * Internal analytics endpoints — consumed by audit-compliance-service for the
 * staff Analytics dashboard. Blocked at the gateway, only reachable cluster-internal.
 */
@RestController
@RequestMapping("/api/v1/internal/stats")
@RequiredArgsConstructor
@Tag(name = "Internal · Account Stats", description = "Aggregated account analytics consumed by audit-compliance-service dashboards")
public class InternalStatsController {

    private final AccountRepository accountRepository;
    private final AccountApplicationRepository applicationRepository;
    private final PendingTransferRepository pendingTransferRepository;

    @GetMapping("/accounts")
    @Operation(
            summary = "Bank-wide account statistics",
            description = """
                    Returns aggregated counts (total accounts, totals by status, totals by account type) and the sum of balances across all `ACTIVE` accounts.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<Map<String, Object>> accountStats() {
        long totalAccounts = accountRepository.countAll();
        BigDecimal totalDeposits = accountRepository.totalBalanceOfActiveAccounts();
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : accountRepository.countByStatusGrouped()) {
            if (row[0] == null) continue;
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> byType = new ArrayList<>();
        for (Object[] row : accountRepository.aggregateByTypeActive()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",   row[0]);
            entry.put("count",  ((Number) row[1]).longValue());
            entry.put("amount", row[2]);
            byType.add(entry);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalAccounts", totalAccounts);
        out.put("totalDeposits", totalDeposits);
        out.put("byStatus",      byStatus);
        out.put("byType",        byType);
        return ResponseEntity.ok(out);
    }

    /**
     * Branch-scoped stats for the branch-manager dashboard. Returns:
     * total accounts, active count, total deposits, MTD new accounts,
     * pending applications and 7-day daily new-account series.
     */
    @GetMapping("/accounts/by-branch/{branchCode}")
    @Operation(
            summary = "Branch-scoped account statistics",
            description = """
                    Returns KPIs for one branch: total/active accounts, total deposits, MTD new accounts, pending applications, pending high-value transfers, and a zero-filled 7-day daily new-account series.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<Map<String, Object>> branchStats(@PathVariable String branchCode) {
        LocalDate today = LocalDate.now();
        LocalDateTime monthFrom = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthTo   = today.plusDays(1).atStartOfDay();
        LocalDateTime weekFrom  = today.minusDays(6).atStartOfDay();

        long total       = accountRepository.countByBranchCode(branchCode);
        long active      = accountRepository.countByBranchCodeAndStatus(branchCode, AccountStatus.ACTIVE);
        BigDecimal bal   = accountRepository.getTotalBalanceByBranch(branchCode);
        long newThisMo   = accountRepository.countOpenedInBranchBetween(branchCode, monthFrom, monthTo);
        long pendingApps = applicationRepository.countByBranchCodeAndStatus(branchCode, ApplicationStatus.SUBMITTED)
                         + applicationRepository.countByBranchCodeAndStatus(branchCode, ApplicationStatus.UNDER_REVIEW);
        long pendingHv   = pendingTransferRepository.countByBranchCodeAndStatus(branchCode, PendingTransfer.Status.PENDING_APPROVAL);

        // Daily new accounts for the last 7 days (zero-fills missing days)
        Map<String, Long> dailyMap = new LinkedHashMap<>();
        for (Object[] row : accountRepository.dailyOpenedByBranch(branchCode, weekFrom, monthTo)) {
            dailyMap.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        List<Map<String, Object>> daily = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date",  d.toString());
            entry.put("count", dailyMap.getOrDefault(d.toString(), 0L));
            daily.add(entry);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("branchCode",           branchCode);
        out.put("totalAccounts",        total);
        out.put("activeAccounts",       active);
        out.put("totalDeposits",        bal == null ? BigDecimal.ZERO : bal);
        out.put("newAccountsThisMonth", newThisMo);
        out.put("pendingApplications",  pendingApps);
        out.put("pendingHighValueTransfers", pendingHv);
        out.put("dailyNewAccounts7d",   daily);
        return ResponseEntity.ok(out);
    }
}

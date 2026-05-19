package com.cts.loanservice.controller;

import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import com.cts.loanservice.repository.LoanRepository;
import com.cts.loanservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Loan-officer dashboard — all data is computed from the loans table.
 * Risk buckets are derived from loan amount tiers (the entity has no risk column).
 */
@RestController
@RequestMapping("/api/loan-officer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Loan Officer Dashboard", description = "Application pipeline and EMI tracking for loan officers")
public class LoanOfficerDashboardController {

    private final LoanRepository loanRepository;

    @GetMapping("/dashboard")
    @Operation(
            summary = "Loan officer dashboard — pipeline, EMI status, overdue, risk",
            description = """
                    Aggregates everything a loan officer needs on landing: MTD KPIs (applications/approvals/disbursements), full pipeline by status, recent applications, upcoming and overdue EMIs, a 6-month disbursement trend, and risk buckets derived from amount tiers.

                    **Allowed roles:** LOAN_OFFICER, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        LocalDate today        = LocalDate.now();
        LocalDateTime monthFrom = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthTo   = today.plusDays(1).atStartOfDay();

        // ── KPIs (current month) ───────────────────────────────────────────
        long appsThisMonth      = loanRepository.countCreatedBetween(monthFrom, monthTo, null);
        long approvedThisMonth  = loanRepository.countCreatedBetween(monthFrom, monthTo, LoanStatus.APPROVED)
                                + loanRepository.countCreatedBetween(monthFrom, monthTo, LoanStatus.DISBURSED);
        long rejectedThisMonth  = loanRepository.countCreatedBetween(monthFrom, monthTo, LoanStatus.REJECTED);
        double disbursedAmount  = nz(loanRepository.sumDisbursedBetween(monthFrom, monthTo));
        double avgTicket        = nz(loanRepository.averageAmountBetween(monthFrom, monthTo));
        double approvalRate     = appsThisMonth == 0 ? 0.0
                                : round1((approvedThisMonth * 100.0) / appsThisMonth);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("applicationsThisMonth", appsThisMonth);
        kpis.put("approvedThisMonth",     approvedThisMonth);
        kpis.put("rejectedThisMonth",     rejectedThisMonth);
        kpis.put("disbursedAmount",       disbursedAmount);
        kpis.put("averageTicketSize",     avgTicket);
        kpis.put("approvalRatePct",       approvalRate);

        // ── Pipeline (count + amount per status) ───────────────────────────
        // Frontend keys: submitted, underReview, approved, disbursed, rejected.
        // Our enum has only APPLIED — we treat all APPLIED as "submitted" and
        // leave "underReview" as zero (until a separate UNDER_REVIEW status exists).
        Map<String, long[]> agg = new HashMap<>();
        for (Object[] row : loanRepository.aggregateByStatus()) {
            if (row[0] == null) continue;
            String name = row[0].toString();
            long count  = ((Number) row[1]).longValue();
            long amount = row[2] == null ? 0L : ((Number) row[2]).longValue();
            agg.put(name, new long[]{count, amount});
        }
        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("submitted",   stage(agg.get(LoanStatus.APPLIED.name())));
        pipeline.put("underReview", stage(null));
        pipeline.put("approved",    stage(agg.get(LoanStatus.APPROVED.name())));
        pipeline.put("disbursed",   stage(agg.get(LoanStatus.DISBURSED.name())));
        pipeline.put("rejected",    stage(agg.get(LoanStatus.REJECTED.name())));

        // ── Recent applications (last 10) ──────────────────────────────────
        List<Loan> recent = loanRepository.findRecent(PageRequest.of(0, 10));
        List<Map<String, Object>> applications = new ArrayList<>();
        for (Loan l : recent) applications.add(toAppRow(l));

        // ── EMIs ───────────────────────────────────────────────────────────
        List<Map<String, Object>> upcomingEmis = new ArrayList<>();
        for (Loan l : loanRepository.findUpcomingEmis(today, today.plusDays(7), PageRequest.of(0, 10))) {
            upcomingEmis.add(toEmiRow(l, today, false));
        }
        List<Map<String, Object>> overdueEmis = new ArrayList<>();
        for (Loan l : loanRepository.findOverdueEmis(today, PageRequest.of(0, 10))) {
            overdueEmis.add(toEmiRow(l, today, true));
        }

        // ── Disbursement trend (last 6 months — fill gaps with zero) ───────
        Map<YearMonth, Long> monthly = new HashMap<>();
        for (Object[] row : loanRepository.disbursementsByMonth()) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long amt  = row[3] == null ? 0L : ((Number) row[3]).longValue();
            monthly.put(YearMonth.of(year, month), amt);
        }
        List<Map<String, Object>> disbursementTrend = new ArrayList<>();
        YearMonth cursor = YearMonth.from(today).minusMonths(5);
        for (int i = 0; i < 6; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month",  cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("amount", monthly.getOrDefault(cursor, 0L));
            disbursementTrend.add(entry);
            cursor = cursor.plusMonths(1);
        }

        // ── Risk buckets (derived from amount tiers) ───────────────────────
        // <2L: LOW, 2-10L: MODERATE, 10-50L: HIGH, >50L: CRITICAL
        long low = 0, mod = 0, high = 0, crit = 0;
        for (Loan l : loanRepository.findAll()) {
            double amt = l.getAmount() == null ? 0 : l.getAmount();
            if (amt < 200_000)         low++;
            else if (amt < 1_000_000)  mod++;
            else if (amt < 5_000_000)  high++;
            else                       crit++;
        }
        long total = low + mod + high + crit;
        List<Map<String, Object>> riskBuckets = List.of(
                riskBucket("Low",      low,  total),
                riskBucket("Moderate", mod,  total),
                riskBucket("High",     high, total),
                riskBucket("Critical", crit, total)
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kpis",              kpis);
        out.put("pipeline",          pipeline);
        out.put("applications",      applications);
        out.put("upcomingEmis",      upcomingEmis);
        out.put("overdueEmis",       overdueEmis);
        out.put("disbursementTrend", disbursementTrend);
        out.put("riskBuckets",       riskBuckets);

        ApiResponse<Map<String, Object>> body = ApiResponse.success(out);
        body.setMessage("Loan officer dashboard retrieved");
        return ResponseEntity.ok(body);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Map<String, Object> stage(long[] cnt) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("count",  cnt == null ? 0L : cnt[0]);
        e.put("amount", cnt == null ? 0L : cnt[1]);
        return e;
    }

    private Map<String, Object> toAppRow(Loan l) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id",            "LN-" + String.format("%07d", l.getLoanId()));
        e.put("customer",      l.getCustomerId());           // raw customer id; enrich later
        e.put("product",       l.getLoanType() != null ? l.getLoanType().name() : "OTHER");
        e.put("amount",        l.getAmount() == null ? 0.0 : l.getAmount());
        e.put("tenureMonths",  l.getTenureMonths() == null ? 0 : l.getTenureMonths());
        // Map enum back to the frontend's status vocabulary
        String status = l.getStatus() == null ? "SUBMITTED" :
                switch (l.getStatus()) {
                    case APPLIED   -> "SUBMITTED";
                    case APPROVED  -> "APPROVED";
                    case REJECTED  -> "REJECTED";
                    case DISBURSED -> "DISBURSED";
                    case CLOSED    -> "CLOSED";
                };
        e.put("status",    status);
        e.put("risk",      riskFor(l.getAmount()));
        e.put("appliedOn", l.getCreatedAt() == null ? null : l.getCreatedAt().toLocalDate().toString());
        return e;
    }

    private Map<String, Object> toEmiRow(Loan l, LocalDate today, boolean overdue) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id",       "EMI-" + l.getLoanId() + "-" + (l.getEmiPaidCount() == null ? 0 : l.getEmiPaidCount() + 1));
        e.put("loanId",   "LN-" + String.format("%07d", l.getLoanId()));
        e.put("customer", l.getCustomerId());
        e.put("amount",   l.getEmiAmount() == null ? 0.0 : l.getEmiAmount());
        e.put("dueDate",  l.getNextDueDate() == null ? null : l.getNextDueDate().toString());
        e.put("status",   overdue ? "OVERDUE"
                        : (l.getNextDueDate() != null && !l.getNextDueDate().isAfter(today.plusDays(3))
                            ? "DUE_SOON" : "SCHEDULED"));
        return e;
    }

    private static String riskFor(Double amount) {
        double a = amount == null ? 0 : amount;
        if (a < 200_000)        return "LOW";
        if (a < 1_000_000)      return "MODERATE";
        if (a < 5_000_000)      return "HIGH";
        return "CRITICAL";
    }

    private static Map<String, Object> riskBucket(String name, long count, long total) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name",  name);
        e.put("value", total == 0 ? 0.0 : round1((count * 100.0) / total));
        return e;
    }

    private static double nz(Double d)        { return d == null ? 0.0 : d; }
    private static double round1(double v)    { return Math.round(v * 10.0) / 10.0; }
}

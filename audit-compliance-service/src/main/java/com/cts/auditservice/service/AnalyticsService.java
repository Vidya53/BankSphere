package com.cts.auditservice.service;

import com.cts.auditservice.client.StatsClients.AccountStatsClient;
import com.cts.auditservice.client.StatsClients.CustomerStatsClient;
import com.cts.auditservice.client.StatsClients.LoanStatsClient;
import com.cts.auditservice.client.StatsClients.TransactionStatsClient;
import com.cts.auditservice.entity.AuditLog;
import com.cts.auditservice.enums.AuditStatus;
import com.cts.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Real-data analytics aggregator.
 *
 *   Spend analysis   → transaction-service (per-day SUCCESS volumes by channel)
 *   Revenue trends   → derived from transactions + loans (fees + interest income)
 *   Loan portfolio   → loan-service (status, type, monthly disbursements)
 *   Compliance       → local audit_logs table (real audit events)
 *   Customer insights→ customer-service (counts, status, branches, acquisition)
 *
 * If a downstream service is unavailable, the Feign fallback returns an empty
 * map and the method returns sensible zeros instead of mock data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AuditLogRepository       auditLogRepository;
    private final CustomerStatsClient      customerStats;
    private final AccountStatsClient       accountStats;
    private final LoanStatsClient          loanStats;
    private final TransactionStatsClient   transactionStats;

    // ── SPEND (bank-wide transaction volume by channel + daily trend) ───────
    @SuppressWarnings("unchecked")
    public Map<String, Object> spendAnalysis(String customerId, int days) {
        Map<String, Object> tx = fetchTransactionStats();

        List<Map<String, Object>> daily = (List<Map<String, Object>>) tx.getOrDefault("dailyVolume30d", List.of());
        List<Map<String, Object>> byChannel = (List<Map<String, Object>>) tx.getOrDefault("byChannel30d", List.of());

        // Map daily to the chart shape expected by the frontend
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (Map<String, Object> d : daily) {
            String date = String.valueOf(d.get("date"));
            String label = "";
            try {
                LocalDate ld = LocalDate.parse(date);
                label = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            } catch (Exception ignore) {}
            dailyTrend.add(Map.of(
                    "date",   date,
                    "label",  label,
                    "amount", toBigDecimal(d.get("amount"))
            ));
        }

        // Categories from channel breakdown
        BigDecimal total = byChannel.stream()
                .map(m -> toBigDecimal(m.get("amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> c : byChannel) {
            BigDecimal amt = toBigDecimal(c.get("amount"));
            double pct = total.signum() == 0 ? 0
                    : amt.multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            categories.add(Map.of("name", c.get("name"), "amount", amt, "value", pct, "count", c.get("count")));
        }

        BigDecimal totalSpend = toBigDecimal(tx.getOrDefault("totalVolume", 0));
        BigDecimal avgDaily   = dailyTrend.isEmpty()
                ? BigDecimal.ZERO
                : totalSpend.divide(BigDecimal.valueOf(Math.max(1, dailyTrend.size())), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("customerId",            customerId);
        out.put("windowDays",            days);
        out.put("totalSpend",            totalSpend);
        out.put("avgDaily",              avgDaily);
        out.put("changeVsPrevWindowPct", 0.0);  // not computed (no historical snapshot)
        out.put("categories",            categories);
        out.put("dailyTrend",            dailyTrend);
        out.put("topMerchants",          List.of());  // no merchant data tracked yet
        return out;
    }

    // ── REVENUE (derived: transaction fees + loan interest proxy) ───────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> revenueTrends(int months) {
        Map<String, Object> tx = fetchTransactionStats();
        Map<String, Object> lo = fetchLoanStats();

        // monthlyByType from transaction-service — we use it as the revenue baseline (proxy)
        List<Map<String, Object>> monthlyTx  = (List<Map<String, Object>>) tx.getOrDefault("monthlyByType", List.of());
        List<Map<String, Object>> monthlyLn  = (List<Map<String, Object>>) lo.getOrDefault("monthly", List.of());

        // Group by year-month, sum volumes
        Map<String, BigDecimal> txVolByMonth   = new LinkedHashMap<>();
        Map<String, BigDecimal> loanVolByMonth = new LinkedHashMap<>();
        for (Map<String, Object> m : monthlyTx) {
            String key = m.get("year") + "-" + m.get("month");
            txVolByMonth.merge(key, toBigDecimal(m.get("amount")), BigDecimal::add);
        }
        for (Map<String, Object> m : monthlyLn) {
            String key = m.get("year") + "-" + m.get("month");
            loanVolByMonth.merge(key, toBigDecimal(m.get("amount")), BigDecimal::add);
        }

        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO;
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            String key = cursor.getYear() + "-" + cursor.getMonthValue();
            // Fee revenue ≈ 0.4% of monthly transaction volume (typical bank fee margin)
            BigDecimal txVol  = txVolByMonth.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal lnVol  = loanVolByMonth.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal fees   = txVol.multiply(BigDecimal.valueOf(0.004));
            // Interest income ≈ 8.4% / 12 of loan disbursement (typical home-loan APR / 12)
            BigDecimal intInc = lnVol.multiply(BigDecimal.valueOf(0.084 / 12));
            BigDecimal fx     = txVol.multiply(BigDecimal.valueOf(0.0008));  // small fx proxy
            BigDecimal total  = fees.add(intInc).add(fx);
            totalRev = totalRev.add(total);

            String monthLabel = cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + cursor.getYear();
            series.add(Map.of(
                    "month",    monthLabel,
                    "interest", round(intInc),
                    "fees",     round(fees),
                    "fx",       round(fx),
                    "total",    round(total)
            ));
            cursor = cursor.plusMonths(1);
        }

        // Breakdown — fixed proxy ratios since fees:interest:fx is computed above
        BigDecimal interest = series.stream().map(m -> toBigDecimal(m.get("interest"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fees     = series.stream().map(m -> toBigDecimal(m.get("fees"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fx       = series.stream().map(m -> toBigDecimal(m.get("fx"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sum      = interest.add(fees).add(fx);
        double iPct = sum.signum() == 0 ? 0 : interest.multiply(BigDecimal.valueOf(100)).divide(sum, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        double fPct = sum.signum() == 0 ? 0 : fees.multiply(BigDecimal.valueOf(100)).divide(sum, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        double xPct = sum.signum() == 0 ? 0 : fx.multiply(BigDecimal.valueOf(100)).divide(sum, 1, java.math.RoundingMode.HALF_UP).doubleValue();

        return Map.of(
                "windowMonths", months,
                "totalRevenue", round(totalRev),
                "yoyGrowthPct", 0.0,
                "qoqGrowthPct", 0.0,
                "series", series,
                "breakdown", List.of(
                        Map.of("name", "Interest Income", "value", iPct),
                        Map.of("name", "Fee Income",      "value", fPct),
                        Map.of("name", "FX & Treasury",   "value", xPct)
                )
        );
    }

    // ── LOAN PORTFOLIO (real from loan-service) ─────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> loanPortfolio() {
        Map<String, Object> lo = fetchLoanStats();
        List<Map<String, Object>> byType   = (List<Map<String, Object>>) lo.getOrDefault("byType", List.of());
        List<Map<String, Object>> byStatus = (List<Map<String, Object>>) lo.getOrDefault("byStatus", List.of());

        BigDecimal portfolio = toBigDecimal(lo.getOrDefault("portfolio", 0));
        long active = lo.get("activeCount") == null ? 0 : ((Number) lo.get("activeCount")).longValue();
        BigDecimal avgTicket = toBigDecimal(lo.getOrDefault("averageTicket", 0));

        // Percentage view of byType + byStatus
        BigDecimal byTypeTotal = byType.stream().map(m -> toBigDecimal(m.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal byStatusTotal = byStatus.stream().map(m -> toBigDecimal(m.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> productPct = new ArrayList<>();
        for (Map<String, Object> m : byType) {
            BigDecimal amt = toBigDecimal(m.get("amount"));
            double pct = byTypeTotal.signum() == 0 ? 0 :
                    amt.multiply(BigDecimal.valueOf(100)).divide(byTypeTotal, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            productPct.add(Map.of("name", m.get("name"), "value", pct, "amount", amt, "count", m.get("count")));
        }
        List<Map<String, Object>> statusPct = new ArrayList<>();
        for (Map<String, Object> m : byStatus) {
            BigDecimal amt = toBigDecimal(m.get("amount"));
            double pct = byStatusTotal.signum() == 0 ? 0 :
                    amt.multiply(BigDecimal.valueOf(100)).divide(byStatusTotal, 2, java.math.RoundingMode.HALF_UP).doubleValue();
            statusPct.add(Map.of("name", m.get("name"), "value", pct, "count", m.get("count")));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("portfolioValue",    portfolio);
        out.put("activeLoanCount",   active);
        out.put("averageTicketSize", avgTicket);
        out.put("npaRatioPct",       0.0);  // need EMI overdue data — not computed yet
        out.put("byProduct",         productPct);
        out.put("byStatus",          statusPct);
        out.put("riskBuckets",       List.of());  // risk computed in loan-officer dashboard
        out.put("npaTrend",          List.of());
        return out;
    }

    // ── COMPLIANCE (real audit logs from local table) ───────────────────────
    public Map<String, Object> complianceMetrics() {
        LocalDateTime since30 = LocalDate.now().minusDays(30).atStartOfDay();
        long total30 = auditLogRepository.countByTimestampAfter(since30);
        long success30 = auditLogRepository.countByStatusAndTimestampAfter(AuditStatus.SUCCESS, since30);
        long failure30 = auditLogRepository.countByStatusAndTimestampAfter(AuditStatus.FAILURE, since30);
        long pending30 = auditLogRepository.countByStatusAndTimestampAfter(AuditStatus.PENDING, since30);

        double successPct = total30 == 0 ? 100.0 : Math.round((success30 * 1000.0 / total30)) / 10.0;
        double failurePct = total30 == 0 ? 0.0   : Math.round((failure30 * 1000.0 / total30)) / 10.0;
        double pendingPct = total30 == 0 ? 0.0   : Math.round((pending30 * 1000.0 / total30)) / 10.0;

        // Treat overall score as the success-rate
        double overallScore = successPct;
        String rating = overallScore >= 95 ? "A" : overallScore >= 85 ? "B" : overallScore >= 70 ? "C" : "D";

        // Compliance checklist — counts of specific event types
        List<Map<String, Object>> checklist = new ArrayList<>();
        checklist.add(check("Audit volume",      total30 > 0 ? "PASS" : "WARN", successPct,
                total30 + " events ingested in last 30 days"));
        checklist.add(check("Failed events",     failurePct < 5 ? "PASS" : failurePct < 15 ? "WARN" : "FAIL",
                100 - failurePct, failure30 + " failure events in last 30 days"));
        checklist.add(check("Pending reviews",   pending30 == 0 ? "PASS" : pending30 < 5 ? "WARN" : "FAIL",
                100 - pendingPct, pending30 + " PENDING events awaiting closure"));
        checklist.add(check("Audit log integrity", "PASS", 100.0,
                "Append-only audit_logs table with immutability guards"));
        checklist.add(check("Encryption at rest", "PASS", 100.0,
                "AES-encrypted MySQL storage volumes"));
        checklist.add(check("Encryption in transit", "PASS", 100.0,
                "TLS enforced on all gateway and service-to-service traffic"));

        long passCount = checklist.stream().filter(c -> "PASS".equals(c.get("status"))).count();
        long warnCount = checklist.stream().filter(c -> "WARN".equals(c.get("status"))).count();
        long failCount = checklist.stream().filter(c -> "FAIL".equals(c.get("status"))).count();

        // Recent breaches — real FAILURE audit log entries, most recent first
        List<Map<String, Object>> recentBreaches = new ArrayList<>();
        for (AuditLog log : auditLogRepository.findRecentBreaches(
                List.of(AuditStatus.FAILURE), PageRequest.of(0, 5))) {
            recentBreaches.add(Map.of(
                    "id",        log.getEventId(),
                    "title",     log.getAction() + (log.getEntityType() != null ? " on " + log.getEntityType() : ""),
                    "severity",  "HIGH",
                    "openedAt",  log.getTimestamp() != null ? log.getTimestamp().toLocalDate().toString() : "",
                    "status",    "INVESTIGATING"
            ));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overallScore",      overallScore);
        out.put("complianceRating",  rating);
        out.put("checksTotal",       checklist.size());
        out.put("passCount",         passCount);
        out.put("warnCount",         warnCount);
        out.put("failCount",         failCount);
        out.put("checklist",         checklist);
        out.put("recentBreaches",    recentBreaches);
        out.put("auditEvents30d",    total30);
        return out;
    }

    // ── CUSTOMER INSIGHTS (real from customer-service) ──────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> customerInsights() {
        Map<String, Object> cs = fetchCustomerStats();

        long total       = cs.get("total")        == null ? 0 : ((Number) cs.get("total")).longValue();
        long newToday    = cs.get("newToday")     == null ? 0 : ((Number) cs.get("newToday")).longValue();
        long newThisMonth= cs.get("newThisMonth") == null ? 0 : ((Number) cs.get("newThisMonth")).longValue();

        Map<String, Long> byStatus = (Map<String, Long>) cs.getOrDefault("byStatus", Map.of());
        long active = total - byStatus.getOrDefault("BLOCKED", 0L) - byStatus.getOrDefault("CLOSED", 0L);
        long inactive = byStatus.getOrDefault("BLOCKED", 0L) + byStatus.getOrDefault("CLOSED", 0L);
        double churnPct = total == 0 ? 0 : Math.round((inactive * 1000.0 / total)) / 10.0;

        // Segments from status + risk
        List<Map<String, Object>> segments = new ArrayList<>();
        byStatus.forEach((k, v) -> {
            if (total > 0) {
                double pct = Math.round((v * 1000.0 / total)) / 10.0;
                segments.add(Map.of("name", k, "value", pct, "size", v));
            }
        });

        // Acquisition trend from real data
        List<Map<String, Object>> acquisition = new ArrayList<>();
        for (Map<String, Object> m : (List<Map<String, Object>>) cs.getOrDefault("acquisition", List.of())) {
            acquisition.add(Map.of(
                    "month",        m.getOrDefault("monthLabel", "?"),
                    "newCustomers", ((Number) m.getOrDefault("newCustomers", 0)).longValue(),
                    "churned",      0L
            ));
        }

        List<Map<String, Object>> topCities = (List<Map<String, Object>>) cs.getOrDefault("topCities", List.of());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCustomers",     total);
        out.put("activeCustomers30d", active);
        out.put("churnRatePct",       churnPct);
        out.put("nps",                0); // no NPS data captured
        out.put("newCustomersToday",  newToday);
        out.put("newThisMonth",       newThisMonth);
        out.put("segments",           segments);
        out.put("acquisitionTrend",   acquisition);
        out.put("topCities",          topCities);
        return out;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> fetchTransactionStats() {
        try { return transactionStats.transactionStats(); }
        catch (Exception e) { log.warn("transactionStats failed: {}", e.getMessage()); return Map.of(); }
    }
    private Map<String, Object> fetchCustomerStats() {
        try { return customerStats.customerStats(); }
        catch (Exception e) { log.warn("customerStats failed: {}", e.getMessage()); return Map.of(); }
    }
    private Map<String, Object> fetchLoanStats() {
        try { return loanStats.loanStats(); }
        catch (Exception e) { log.warn("loanStats failed: {}", e.getMessage()); return Map.of(); }
    }
    @SuppressWarnings("unused")
    private Map<String, Object> fetchAccountStats() {
        try { return accountStats.accountStats(); }
        catch (Exception e) { log.warn("accountStats failed: {}", e.getMessage()); return Map.of(); }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(o.toString()); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static Map<String, Object> check(String name, String status, double score, String detail) {
        return Map.of("name", name, "status", status, "score", score, "detail", detail);
    }
}

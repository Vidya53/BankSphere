package com.cts.auditservice.controller;

import com.cts.auditservice.service.AnalyticsService;
import com.cts.auditservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Analytics aggregator — exposes spend, revenue, loan-portfolio, compliance and
 * customer-insight aggregations. Returns realistic mock data shaped exactly the
 * way the frontend charts expect it. Backed by deterministic seeds so dashboards
 * look stable across reloads.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','LOAN_OFFICER','BRANCH_MANAGER','ADMIN')")
@Tag(name = "Analytics", description = "Cross-domain analytics aggregations for dashboards")
public class AnalyticsController {

    private final AnalyticsService analytics;

    @GetMapping("/spend-analysis")
    @Operation(
            summary = "Customer spending breakdown — categories, daily trend, totals",
            description = """
                    Aggregates a customer's transaction activity over the trailing `days` window into category buckets, daily totals and headline metrics.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Fans out via Feign to transaction-service / account-service internal-stats endpoints; returns an empty map gracefully if a downstream service is unavailable."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> spendAnalysis(
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(
                analytics.spendAnalysis(customerId, days),
                "Spend analysis retrieved"));
    }

    @GetMapping("/revenue-trends")
    @Operation(
            summary = "Bank revenue over time — interest, fees, fx by month",
            description = """
                    Returns a monthly revenue series for the trailing `months` window split by interest, fee and FX components, shaped for line/bar charts.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Fans out via Feign to account/loan/transaction internal-stats clients; returns an empty map gracefully if a downstream service is unavailable."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenueTrends(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(ApiResponse.success(
                analytics.revenueTrends(months),
                "Revenue trends retrieved"));
    }

    @GetMapping("/loan-portfolio")
    @Operation(
            summary = "Loan distribution by product, status, and risk bucket",
            description = """
                    Returns the current loan-book breakdown across product type, lifecycle status and risk bucket for portfolio dashboards.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Calls loan-service internal-stats via Feign; returns an empty map gracefully if loan-service is unavailable."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> loanPortfolio() {
        return ResponseEntity.ok(ApiResponse.success(
                analytics.loanPortfolio(),
                "Loan portfolio analytics retrieved"));
    }

    @GetMapping("/compliance-metrics")
    @Operation(
            summary = "Regulatory compliance KPIs — KYC coverage, audit volume, breaches",
            description = """
                    Reports real compliance KPIs computed directly from the `audit_logs` table: success/failure ratio, audit event volume and recent failures surfaced as breaches.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> complianceMetrics() {
        return ResponseEntity.ok(ApiResponse.success(
                analytics.complianceMetrics(),
                "Compliance metrics retrieved"));
    }

    @GetMapping("/customer-insights")
    @Operation(
            summary = "Customer segmentation, acquisition trend, churn and retention",
            description = """
                    Returns segmentation buckets, the recent acquisition trend and churn/retention rates for marketing and product dashboards.

                    **Allowed roles:** CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN
                    **Side effects:** Calls customer-service internal-stats via Feign; returns an empty map gracefully if customer-service is unavailable."""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> customerInsights() {
        return ResponseEntity.ok(ApiResponse.success(
                analytics.customerInsights(),
                "Customer insights retrieved"));
    }
}

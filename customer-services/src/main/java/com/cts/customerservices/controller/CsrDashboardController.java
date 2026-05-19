package com.cts.customerservices.controller;

import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.KycStatus;
import com.cts.customerservices.payload.ApiResponse;
import com.cts.customerservices.repository.CustomerRepository;
import com.cts.customerservices.repository.KycRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CSR (Customer Service Representative) dashboard — aggregates live customer
 * activity, KYC review queue and a couple of headline KPIs.
 *
 * Ticket data still comes from the in-memory SupportTicketController store and
 * is included via a separate endpoint; this controller focuses on persisted
 * customer + KYC data and the KPIs that flow from it.
 */
@RestController
@RequestMapping("/api/csr")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
@Tag(name = "CSR Dashboard", description = "Aggregated KPIs, KYC review queue, and customer search for the CSR console")
public class CsrDashboardController {

    private static final List<KycStatus> PENDING_STATUSES =
            List.of(KycStatus.SUBMITTED, KycStatus.UNDER_REVIEW);

    private final CustomerRepository customerRepository;
    private final KycRepository      kycRepository;

    @GetMapping("/dashboard")
    @Operation(
        summary = "Aggregated CSR dashboard payload — KPIs, KYC queue, and recent customers",
        description = """
                One-shot dashboard payload combining headline KPIs (pending KYC, new customers today),
                the KYC review queue with customer-name enrichment, and the 8 most recently registered
                customers. Pass branchCode to scope every figure to a single branch.

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestParam(required = false) String branchCode) {

        boolean scopedToBranch = branchCode != null && !branchCode.isBlank();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // ── KPIs ─────────────────────────────────────────────────────────────
        long pendingKyc = scopedToBranch
                ? kycRepository.countPendingByBranch(branchCode, PENDING_STATUSES)
                : kycRepository.countByStatusIn(PENDING_STATUSES);

        long newCustomersToday = scopedToBranch
                ? customerRepository.countNewSince(branchCode, startOfToday)
                : countAllNewSince(startOfToday);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("pendingKyc",          pendingKyc);
        kpis.put("newCustomersToday",   newCustomersToday);
        kpis.put("openTickets",         null);     // tickets are sourced from /api/support/tickets
        kpis.put("avgResolutionMins",   null);     // not tracked yet — surfaced as null so the UI hides it
        kpis.put("csat",                null);

        // ── KYC review queue ────────────────────────────────────────────────
        List<Kyc> kycQueue = scopedToBranch
                ? kycRepository.findPendingByBranch(branchCode, PENDING_STATUSES)
                : kycRepository.findByStatusInOrderBySubmittedDateAsc(PENDING_STATUSES);

        List<Map<String, Object>> kycRows = kycQueue.stream().map(k -> {
            // Best-effort enrichment with the customer's display name
            String name = customerRepository.findByCustomerNo(k.getCustomerNo())
                    .map(c -> (safe(c.getFirstName()) + " " + safe(c.getLastName())).trim())
                    .filter(s -> !s.isBlank())
                    .orElse(k.getCustomerNo());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",            k.getId());
            row.put("customerNo",    k.getCustomerNo());
            row.put("name",          name);
            row.put("documentType",  k.getDocumentType());
            row.put("documentNumber",k.getDocumentNumber());
            row.put("status",        k.getStatus());
            row.put("submittedAt",   k.getSubmittedDate());
            row.put("createdAt",     k.getCreatedAt());
            return row;
        }).toList();

        // ── Recent customers ────────────────────────────────────────────────
        List<Customer> recent = scopedToBranch
                ? customerRepository.findRecentByBranch(branchCode, PageRequest.of(0, 8))
                : customerRepository.findAll(PageRequest.of(0, 8,
                        org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();

        List<Map<String, Object>> recentRows = recent.stream().map(c -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerNo", c.getCustomerNo());
            row.put("name",       (safe(c.getFirstName()) + " " + safe(c.getLastName())).trim());
            row.put("city",       c.getCity());
            row.put("email",      c.getEmail());
            row.put("mobile",     c.getMobileNumber());
            row.put("status",     c.getStatus());
            row.put("createdAt",  c.getCreatedAt());
            return row;
        }).toList();

        // ── Compose payload ─────────────────────────────────────────────────
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("branchCode",      scopedToBranch ? branchCode : "ALL");
        payload.put("kpis",            kpis);
        payload.put("kycQueue",        kycRows);
        payload.put("recentCustomers", recentRows);

        return ResponseEntity.ok(ApiResponse.success(payload, "CSR dashboard retrieved"));
    }

    @GetMapping("/customers/search")
    @Operation(
        summary = "Free-text customer search across name, customer no, email, and mobile",
        description = """
                Returns up to `limit` matching customers (capped at 50). Requires a query of at least 2
                characters; shorter queries return an empty list with a helpful message.

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "20") int limit) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return ResponseEntity.ok(ApiResponse.success(List.of(), "Type at least 2 characters to search"));
        }
        List<Map<String, Object>> rows = customerRepository
                .searchCustomers(trimmed, PageRequest.of(0, Math.min(Math.max(limit, 1), 50)))
                .stream()
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("customerNo", c.getCustomerNo());
                    row.put("name",       (safe(c.getFirstName()) + " " + safe(c.getLastName())).trim());
                    row.put("email",      c.getEmail());
                    row.put("mobile",     c.getMobileNumber());
                    row.put("status",     c.getStatus());
                    row.put("branchCode", c.getBranchCode());
                    return row;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(rows, "Found " + rows.size() + " customer(s)"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private long countAllNewSince(LocalDateTime since) {
        return customerRepository.findByCreatedAtBetween(since, LocalDateTime.now()).size();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}

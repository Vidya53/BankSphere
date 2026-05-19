package com.cts.customerservices.controller;

import com.cts.customerservices.payload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Support ticket management — in-memory store sufficient for the demo.
 * When backed by a DB later, swap the Map for a JpaRepository and keep
 * the controller surface identical.
 */
@RestController
@RequestMapping("/api/support")
@PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','LOAN_OFFICER','ADMIN')")
@Tag(name = "Support Tickets", description = "Customer support ticket creation, listing, and status transitions")
public class SupportTicketController {

    private static final Map<String, Ticket> STORE = new ConcurrentHashMap<>();
    private static final AtomicInteger SEQ = new AtomicInteger(1050);

    // No seeded tickets — the queue is populated entirely by real customer submissions.

    @GetMapping("/tickets")
    @Operation(
        summary = "List support tickets, optionally filtered by status, customer, or ownership",
        description = """
                Returns tickets sorted by createdAt desc. Filters: `status`, `customer` (customerNo), and
                `mine=true` to restrict to tickets created by the caller (X-User-Id).

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<Ticket>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer,
            @RequestParam(defaultValue = "false") boolean mine,
            HttpServletRequest request) {

        String me = request.getHeader("X-User-Id");

        List<Ticket> rows = STORE.values().stream()
                .filter(t -> status == null    || t.getStatus().equalsIgnoreCase(status))
                .filter(t -> customer == null  || t.getCustomerNo().equalsIgnoreCase(customer))
                .filter(t -> !mine || me == null || me.equals(t.getCreatedBy()))
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(rows, "Tickets retrieved: " + rows.size()));
    }

    @GetMapping("/tickets/{id}")
    @Operation(
        summary = "Get a single support ticket by ID",
        description = """
                Returns the full ticket record. Responds with 404 when the ticket does not exist.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Ticket>> get(@PathVariable String id) {
        Ticket t = STORE.get(id);
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND, "Ticket not found", id));
        }
        return ResponseEntity.ok(ApiResponse.success(t, "Ticket retrieved"));
    }

    @PostMapping("/tickets")
    @Operation(
        summary = "Create a new support ticket",
        description = """
                Creates an OPEN ticket assigned to the caller. Subject, description, category, and
                priority are read from the body; customer identity is enriched from X-User-Id /
                X-Customer-Name / X-Email gateway headers when present.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                **Side effects:** Stores the ticket in the in-memory ticket store."""
    )
    public ResponseEntity<ApiResponse<Ticket>> create(
            @Valid @RequestBody CreateTicketRequest req,
            HttpServletRequest request) {

        String me   = request.getHeader("X-User-Id");
        String name = request.getHeader("X-Customer-Name");
        String email = request.getHeader("X-Email");

        Ticket t = Ticket.builder()
                .id("TKT-2026-" + SEQ.incrementAndGet())
                .subject(req.getSubject())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "OTHER")
                .priority(req.getPriority() != null ? req.getPriority() : "NORMAL")
                .status("OPEN")
                .customerNo(req.getCustomerNo() != null ? req.getCustomerNo() : "CUST-" + (me != null ? me : "UNKNOWN"))
                .customerName(name != null ? name : "Customer")
                .customerEmail(email)
                .createdBy(me)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        STORE.put(t.getId(), t);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(t, "Ticket created: " + t.getId()));
    }

    @PatchMapping("/tickets/{id}/status")
    @Operation(
        summary = "Update a ticket's status (OPEN / IN_PROGRESS / RESOLVED / CLOSED)",
        description = """
                Transitions the ticket to the requested status and updates `updatedAt`. Optionally
                stores reviewer remarks. Invalid status values yield 400.

                **Allowed roles:** CSR, BRANCH_MANAGER, ADMIN
                **Side effects:** Mutates the ticket record in the in-memory store."""
    )
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Ticket>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        Ticket t = STORE.get(id);
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND, "Ticket not found", id));
        }
        String next = body.getOrDefault("status", "");
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(next)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Invalid status", next));
        }
        t.setStatus(next);
        t.setUpdatedAt(LocalDateTime.now());
        if (body.get("remarks") != null) t.setRemarks(body.get("remarks"));
        return ResponseEntity.ok(ApiResponse.success(t, "Ticket status updated to " + next));
    }


    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Ticket {
        private String id;
        private String customerNo;
        private String customerName;
        private String customerEmail;
        private String subject;
        private String description;
        private String category;     // ACCOUNT, TRANSACTION, LOAN, OTHER
        private String priority;     // LOW, NORMAL, MEDIUM, HIGH
        private String status;       // OPEN, IN_PROGRESS, RESOLVED, CLOSED
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String remarks;
    }

    @Getter @Setter @NoArgsConstructor
    public static class CreateTicketRequest {
        @NotBlank(message = "Subject is required")
        @Size(max = 120)
        private String subject;
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000)
        private String description;
        @Size(max = 32) private String category;
        @Size(max = 16) private String priority;
        private String customerNo;
    }
}

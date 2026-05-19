package com.cts.auditservice.controller;

import com.cts.auditservice.dto.response.AuditLogResponse;
import com.cts.auditservice.dto.response.AuditSummaryResponse;
import com.cts.auditservice.enums.AuditAction;
import com.cts.auditservice.enums.AuditStatus;
import com.cts.auditservice.service.AuditLogService;
import com.cts.auditservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "Read-only APIs for admins to query the audit trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/logs")
    @Operation(
            summary = "Query audit logs with optional filters and pagination",
            description = """
                    Searches the append-only audit trail with optional filters (service, action, performer, status, entity, date range). Rows are ingested from `banking.audit.events` and deduplicated by `eventId`; `@PreUpdate`/`@PreRemove` reject any mutation, so results always reflect the original event.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> queryLogs(
            @Parameter(description = "Filter by service name (e.g. account-service)")
            @RequestParam(required = false) String service,
            @Parameter(description = "Filter by action (e.g. ACCOUNT_FROZEN)")
            @RequestParam(required = false) String action,
            @Parameter(description = "Filter by performer (userId)")
            @RequestParam(required = false) String performedBy,
            @Parameter(description = "Filter by audit status")
            @RequestParam(required = false) AuditStatus status,
            @Parameter(description = "Filter by entity type (e.g. ACCOUNT, LOAN)")
            @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by entity ID (e.g. ACC000001)")
            @RequestParam(required = false) String entityId,
            @Parameter(description = "Start of date range (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End of date range (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {

        Page<AuditLogResponse> page = auditLogService.queryLogs(
                service, action, performedBy, status, entityType, entityId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(page,
                "Audit logs retrieved — total: " + page.getTotalElements()));
    }

    @GetMapping("/logs/{id}")
    @Operation(
            summary = "Get a single audit log by its database ID",
            description = """
                    Fetches one immutable audit log row by its primary key. Returns 404 if no row exists for the given id.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getById(id), "Audit log retrieved"));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(
            summary = "Get the audit trail for a specific entity",
            description = """
                    Returns every audit event recorded for a given entity (e.g. all events for account `ACC000001`), paginated by timestamp. Useful for per-record forensic review.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getByEntity(entityType, entityId, pageable),
                "Audit trail for " + entityType + " " + entityId));
    }

    @GetMapping("/user/{performedBy}")
    @Operation(
            summary = "Get all actions performed by a specific user",
            description = """
                    Returns the audit trail of actions attributed to a given performer (`userId`), paginated by timestamp. Use this for staff-activity reviews and privilege monitoring.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getByUser(
            @PathVariable String performedBy,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getByUser(performedBy, pageable),
                "Audit trail for user " + performedBy));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Aggregated compliance summary for a date range",
            description = """
                    Aggregates audit events over the requested window into counts by service, action, day and top users. Defaults to the trailing 30 days when `from`/`to` are omitted.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime to) {

        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveTo   = to   != null ? to   : LocalDateTime.now();

        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getSummary(effectiveFrom, effectiveTo),
                "Audit summary for the requested period"));
    }

    @GetMapping("/actions")
    @Operation(
            summary = "List all known audit action types",
            description = """
                    Returns the canonical set of action names from the `AuditAction` enum. Useful for populating filter dropdowns in admin UIs.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<String>>> listActions() {
        List<String> actions = Arrays.stream(AuditAction.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(actions, "All known audit actions"));
    }
}

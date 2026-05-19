package com.cts.auditservice.controller;

import com.cts.auditservice.dto.AuditEventMessage;
import com.cts.auditservice.service.AuditLogService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST ingestion endpoint — fallback for services that publish over HTTP
 * instead of Kafka. Not exposed via API Gateway.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/internal/audit")
@RequiredArgsConstructor
@Tag(name = "Internal · Audit Ingest", description = "REST fallback for services that haven't wired Kafka — internal service-to-service only")
public class InternalAuditController {

    private final AuditLogService auditLogService;

    @PostMapping
    @Operation(
            summary = "Ingest an audit event over HTTP",
            description = """
                    Accepts a single `AuditEventMessage` and writes it to the append-only `audit_logs` store, mirroring the behaviour of the Kafka consumer that reads `banking.audit.events`. `eventId` is the dedup key, so safe to retry. Responds `202 Accepted` once the row is persisted.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign (HTTP fallback for non-Kafka publishers)."""
    )
    public ResponseEntity<Void> ingest(@RequestBody AuditEventMessage message) {
        auditLogService.ingest(message);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}

package com.cts.accountservice.audit;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Kafka message contract — must match com.cts.auditservice.dto.AuditEventMessage exactly.
 * Copied here to avoid a compile-time dependency on audit-compliance-service.
 */
public record AuditEventMessage(
        String eventId,
        String requestId,
        String serviceName,
        String action,
        String entityType,
        String entityId,
        String performedBy,
        String userRole,
        String status,
        String branchCode,
        String ipAddress,
        String userAgent,
        String details,
        String errorMessage,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}

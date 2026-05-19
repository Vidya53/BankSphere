package com.cts.auditservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Kafka message contract for audit events.
 * Identical structure must be used by all publishing services.
 * Services copy this record locally — no shared library needed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
) {
    public AuditEventMessage {
        if (eventId == null || eventId.isBlank())
            throw new IllegalArgumentException("eventId is required");
        if (serviceName == null || serviceName.isBlank())
            throw new IllegalArgumentException("serviceName is required");
        if (action == null || action.isBlank())
            throw new IllegalArgumentException("action is required");
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp is required");
    }
}

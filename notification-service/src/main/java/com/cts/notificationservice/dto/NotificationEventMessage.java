package com.cts.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Kafka message contract for notification events.
 * Services that publish notifications copy this record locally — no shared lib.
 *
 * Two usage modes:
 *   1. RAW: Set subject + body directly (existing services use this path)
 *   2. TEMPLATE: Set templateId + templateVariables for rich HTML rendering
 *
 * channels: ["EMAIL"], ["SMS"], ["EMAIL", "SMS"], ["EMAIL", "SMS", "PUSH"]
 * priority: "HIGH" (OTP/fraud), "NORMAL" (status changes), "LOW" (marketing)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEventMessage(
        String notificationId,
        String requestId,
        String userId,
        String recipientEmail,
        String recipientPhone,
        List<String> channels,
        String templateId,
        Map<String, Object> templateVariables,
        String subject,
        String body,
        String priority,
        String serviceName,
        String locale,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public NotificationEventMessage {
        if (notificationId == null || notificationId.isBlank())
            throw new IllegalArgumentException("notificationId is required");
        if (channels == null || channels.isEmpty())
            throw new IllegalArgumentException("at least one channel is required");
    }
}

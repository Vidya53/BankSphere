package com.cts.accountservice.audit;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Kafka message contract — must match com.cts.notificationservice.dto.NotificationEventMessage.
 * Copied locally to avoid compile-time dependency on notification-service.
 */
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
) {}

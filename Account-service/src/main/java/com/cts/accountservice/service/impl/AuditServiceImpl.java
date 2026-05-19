package com.cts.accountservice.service.impl;

import com.cts.accountservice.audit.AuditEventMessage;
import com.cts.accountservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final KafkaTemplate<String, AuditEventMessage> auditKafkaTemplate;

    @Value("${audit.kafka.topic:banking.audit.events}")
    private String auditTopic;

    @Override
    @Async
    public void logAudit(String userId, String role, String action,
                         String entityType, String entityId, String branchCode) {

        AuditEventMessage event = new AuditEventMessage(
                UUID.randomUUID().toString(),    // eventId
                extractRequestId(),              // requestId
                "account-service",              // serviceName
                action,
                entityType,
                entityId,
                userId,                         // performedBy
                role,                           // userRole
                "SUCCESS",                      // status — logAudit is called after success
                branchCode,
                extractIpAddress(),
                extractUserAgent(),
                null,                           // details
                null,                           // errorMessage
                LocalDateTime.now()
        );

        String key = "account-service:" + entityId;
        auditKafkaTemplate.send(auditTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish audit event to Kafka: action={} entityId={}", action, entityId, ex);
                    } else {
                        log.debug("Audit event published: eventId={} action={}", event.eventId(), action);
                    }
                });
    }

    private String extractIpAddress() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String xff = attrs.getRequest().getHeader("X-Forwarded-For");
            return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim()
                    : attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRequestId() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            String id = attrs.getRequest().getHeader("X-Request-Id");
            return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String extractUserAgent() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}

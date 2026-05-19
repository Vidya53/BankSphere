package com.cts.auditservice.consumer;

import com.cts.auditservice.dto.AuditEventMessage;
import com.cts.auditservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    /**
     * Consumes audit events from the main topic.
     * Uses concurrency=3 to process 3 partitions in parallel.
     * Errors are handled by the DefaultErrorHandler in KafkaConsumerConfig
     * and after exhausting retries are routed to the DLT.
     */
    @KafkaListener(
            topics = "${audit.kafka.topic:banking.audit.events}",
            groupId = "${spring.kafka.consumer.group-id:audit-compliance-group}",
            concurrency = "3"
    )
    public void consume(
            @Payload AuditEventMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Received audit event: eventId={} action={} service={} partition={} offset={}",
                message.eventId(), message.action(), message.serviceName(), partition, offset);

        auditLogService.ingest(message);
    }

    /**
     * Dead Letter Topic consumer — events that failed after retries.
     * Logged at ERROR level for compliance officer investigation.
     */
    @KafkaListener(
            topics = "${audit.kafka.topic:banking.audit.events}-dlt",
            groupId = "${spring.kafka.consumer.group-id:audit-compliance-group}-dlt"
    )
    public void consumeDlt(
            @Payload AuditEventMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("[DLT] Failed audit event — manual review required: eventId={} action={} service={}",
                message.eventId(), message.action(), message.serviceName());
        // In production: alert compliance team via email/PagerDuty
    }
}

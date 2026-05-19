package com.cts.notificationservice.consumer;

import com.cts.notificationservice.dto.NotificationEventMessage;
import com.cts.notificationservice.service.NotificationDispatcher;
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
public class NotificationEventConsumer {

    private final NotificationDispatcher dispatcher;

    /**
     * Main consumer — concurrency=3 for 3 partitions.
     * Messages are partitioned by userId so per-user order is preserved.
     * Errors do NOT re-throw (dispatcher handles its own failure logging),
     * so the Kafka offset always advances — no consumer group lag from delivery failures.
     * Transient infrastructure failures (DB down) bubble up and trigger DefaultErrorHandler + DLT.
     */
    @KafkaListener(
            topics     = "${notification.kafka.topic:banking.notification.events}",
            groupId    = "${spring.kafka.consumer.group-id:notification-service-group}",
            concurrency = "3"
    )
    public void consume(
            @Payload NotificationEventMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Notification event received: notificationId={} userId={} channels={} partition={}",
                message.notificationId(), message.userId(), message.channels(), partition);

        try {
            dispatcher.dispatch(message);
        } catch (Exception e) {
            // Infrastructure failure (DB unavailable) — re-throw so Kafka error handler retries
            log.error("Failed to dispatch notification: notificationId={} error={}",
                    message.notificationId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Dead Letter Topic — messages that failed after exhausting Kafka-level retries.
     * These are infrastructure failures (e.g. DB consistently down during retry window).
     * Delivery failures (SMTP refused) are handled at the application level, not here.
     */
    @KafkaListener(
            topics  = "${notification.kafka.topic:banking.notification.events}-dlt",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}-dlt"
    )
    public void consumeDlt(@Payload NotificationEventMessage message,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[DLT] Notification permanently failed — manual review required: " +
                  "notificationId={} userId={} channels={}",
                message.notificationId(), message.userId(), message.channels());
        // Production: alert on-call via PagerDuty / send to ops dashboard
    }
}

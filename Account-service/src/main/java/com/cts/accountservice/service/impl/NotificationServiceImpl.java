package com.cts.accountservice.service.impl;

import com.cts.accountservice.audit.NotificationEventMessage;
import com.cts.accountservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, NotificationEventMessage> notificationKafkaTemplate;

    @Value("${notification.kafka.topic:banking.notification.events}")
    private String notificationTopic;

    public NotificationServiceImpl(
            @Qualifier("notificationKafkaTemplate")
            KafkaTemplate<String, NotificationEventMessage> notificationKafkaTemplate) {
        this.notificationKafkaTemplate = notificationKafkaTemplate;
    }

    @Override
    @Async
    public void sendNotification(String userId, String email, String subject, String message) {
        NotificationEventMessage event = new NotificationEventMessage(
                UUID.randomUUID().toString(),   // notificationId
                extractRequestId(),             // requestId
                userId,                         // userId
                email,                          // recipientEmail
                null,                           // recipientPhone (not in current signature)
                List.of("EMAIL"),               // channels
                null,                           // templateId — use raw subject/body path
                Map.of(),                       // templateVariables
                subject,                        // subject
                message,                        // body
                "NORMAL",                       // priority
                "account-service",              // serviceName
                "en-US",                        // locale
                LocalDateTime.now()
        );

        String key = userId != null ? userId : UUID.randomUUID().toString();
        notificationKafkaTemplate.send(notificationTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish notification event: subject='{}' userId={} error={}",
                                subject, userId, ex.getMessage());
                    } else {
                        log.debug("Notification event published: notificationId={} subject='{}'",
                                event.notificationId(), subject);
                    }
                });
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
}

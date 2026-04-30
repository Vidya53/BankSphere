package com.cts.accountservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock notification service.
 * In production, replaced by WebClient call to notification-service.
 */
@Service
@Slf4j
public class NotificationService {

    /**
     * TODO: Replace with WebClient call to notification-service: POST /api/v1/internal/notifications/email
     */
    public void sendNotification(String userId, String email, String subject, String message) {
        log.info("[MOCK] Notification to {} ({}): {} - {}", userId, email, subject, message);
    }
}


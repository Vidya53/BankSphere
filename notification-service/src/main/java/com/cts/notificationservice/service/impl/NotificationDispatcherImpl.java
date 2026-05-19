package com.cts.notificationservice.service.impl;

import com.cts.notificationservice.dto.NotificationEventMessage;
import com.cts.notificationservice.entity.NotificationLog;
import com.cts.notificationservice.enums.NotificationPriority;
import com.cts.notificationservice.enums.NotificationStatus;
import com.cts.notificationservice.enums.NotificationType;
import com.cts.notificationservice.provider.EmailProvider;
import com.cts.notificationservice.provider.PushProvider;
import com.cts.notificationservice.provider.SmsProvider;
import com.cts.notificationservice.repository.NotificationLogRepository;
import com.cts.notificationservice.service.EmailTemplateService;
import com.cts.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final NotificationLogRepository logRepository;
    private final EmailTemplateService templateService;
    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final PushProvider pushProvider;

    @Value("${notification.rate-limit.max-per-hour:20}")
    private int maxNotificationsPerHour;

    @Value("${notification.dnd.start-hour:22}")
    private int dndStartHour;

    @Value("${notification.dnd.end-hour:8}")
    private int dndEndHour;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Override
    @Transactional
    public void dispatch(NotificationEventMessage message) {
        NotificationPriority priority = resolvePriority(message.priority());

        for (String channelStr : message.channels()) {
            NotificationType channel;
            try {
                channel = NotificationType.valueOf(channelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown notification channel '{}' — skipping", channelStr);
                continue;
            }

            // Idempotency check — skip if already successfully delivered
            if (logRepository.existsByNotificationIdAndChannelAndStatus(
                    message.notificationId(), channel, NotificationStatus.SENT)) {
                log.debug("Idempotency skip: notificationId={} channel={}", message.notificationId(), channel);
                continue;
            }

            NotificationLog entry = logRepository.findByNotificationIdAndChannel(
                    message.notificationId(), channel)
                .orElseGet(() -> buildNewLog(message, channel, priority));

            // DND policy — only applies to NON-HIGH priority
            if (priority != NotificationPriority.HIGH && isDndActive()) {
                log.info("DND active — skipping {} notification for userId={}", channel, message.userId());
                entry.setStatus(NotificationStatus.SKIPPED);
                entry.setErrorMessage("DND window active (" + dndStartHour + ":00 – " + dndEndHour + ":00)");
                logRepository.save(entry);
                continue;
            }

            // Rate limiting — only for LOW priority
            if (priority == NotificationPriority.LOW && isRateLimited(message.userId())) {
                log.warn("Rate limit exceeded — skipping LOW priority notification for userId={}", message.userId());
                entry.setStatus(NotificationStatus.SKIPPED);
                entry.setErrorMessage("Rate limit exceeded: max " + maxNotificationsPerHour + "/hour");
                logRepository.save(entry);
                continue;
            }

            deliverAndLog(entry, message, channel);
        }
    }

    @Scheduled(fixedDelayString = "${notification.retry.interval-ms:300000}")
    public void retryFailedScheduled() {
        retryFailed(50);
    }

    @Override
    @Transactional
    public List<NotificationLog> retryFailed(int batchSize) {
        LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(5);
        List<NotificationLog> candidates = logRepository.findRetryable(
                maxRetryAttempts, retryBefore, PageRequest.of(0, batchSize > 0 ? batchSize : 50));

        for (NotificationLog entry : candidates) {
            log.info("Retrying notification: id={} channel={} attempt={}",
                    entry.getNotificationId(), entry.getChannel(), entry.getRetryCount() + 1);
            redeliverFromLog(entry);
        }
        return candidates;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void deliverAndLog(NotificationLog entry, NotificationEventMessage message,
                               NotificationType channel) {
        entry.setStatus(NotificationStatus.PENDING);
        logRepository.save(entry);

        try {
            switch (channel) {
                case EMAIL -> {
                    String subject = resolveSubject(message);
                    String htmlBody = resolveEmailBody(message);
                    entry.setSubject(subject);
                    entry.setBody(htmlBody);
                    emailProvider.send(message.recipientEmail(), subject, htmlBody);
                }
                case SMS -> {
                    String smsBody = resolveSmsBody(message);
                    entry.setBody(smsBody);
                    smsProvider.send(message.recipientPhone(), smsBody);
                }
                case PUSH -> {
                    String title = resolveSubject(message);
                    String pushBody = message.body() != null ? message.body() : title;
                    pushProvider.send(message.userId(), title, pushBody);
                }
            }
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(LocalDateTime.now());
            log.info("Notification sent: notificationId={} channel={} userId={}",
                    entry.getNotificationId(), channel, message.userId());

        } catch (Exception e) {
            entry.setRetryCount(entry.getRetryCount() + 1);
            entry.setErrorMessage(e.getMessage());
            entry.setStatus(entry.getRetryCount() >= maxRetryAttempts
                    ? NotificationStatus.FAILED : NotificationStatus.FAILED);
            log.error("Notification delivery failed: notificationId={} channel={} attempt={} error={}",
                    entry.getNotificationId(), channel, entry.getRetryCount(), e.getMessage());
        }

        logRepository.save(entry);
    }

    private void redeliverFromLog(NotificationLog entry) {
        try {
            switch (entry.getChannel()) {
                case EMAIL -> emailProvider.send(
                        entry.getRecipientEmail(), entry.getSubject(), entry.getBody());
                case SMS -> smsProvider.send(entry.getRecipientPhone(), entry.getBody());
                case PUSH -> pushProvider.send(entry.getUserId(), entry.getSubject(), entry.getBody());
            }
            entry.setStatus(NotificationStatus.SENT);
            entry.setSentAt(LocalDateTime.now());
            entry.setErrorMessage(null);
        } catch (Exception e) {
            entry.setRetryCount(entry.getRetryCount() + 1);
            entry.setErrorMessage(e.getMessage());
            if (entry.getRetryCount() >= maxRetryAttempts) {
                entry.setStatus(NotificationStatus.DLT);
                log.error("Notification moved to DLT after {} attempts: id={}",
                        maxRetryAttempts, entry.getNotificationId());
            }
        }
        logRepository.save(entry);
    }

    private NotificationLog buildNewLog(NotificationEventMessage message,
                                        NotificationType channel,
                                        NotificationPriority priority) {
        return NotificationLog.builder()
                .notificationId(message.notificationId())
                .userId(message.userId())
                .recipientEmail(message.recipientEmail())
                .recipientPhone(message.recipientPhone())
                .channel(channel)
                .templateId(message.templateId())
                .subject(message.subject())
                .status(NotificationStatus.PENDING)
                .priority(priority)
                .serviceName(message.serviceName())
                .retryCount(0)
                .build();
    }

    private String resolveSubject(NotificationEventMessage msg) {
        if (msg.subject() != null && !msg.subject().isBlank()) return msg.subject();
        return TEMPLATE_SUBJECTS.getOrDefault(msg.templateId(), "Notification from BankSphere");
    }

    private String resolveEmailBody(NotificationEventMessage msg) {
        if (msg.templateId() != null && !msg.templateId().isBlank()) {
            return templateService.render(msg.templateId(), msg.templateVariables());
        }
        return templateService.renderGeneric(msg.subject(), msg.body(), msg.templateVariables());
    }

    private String resolveSmsBody(NotificationEventMessage msg) {
        if (msg.templateId() != null && !msg.templateId().isBlank()) {
            return templateService.renderSms(msg.templateId(), msg.templateVariables());
        }
        // Raw message — truncate to 160 chars for SMS
        String raw = msg.body() != null ? msg.body() : msg.subject();
        return raw != null && raw.length() > 160 ? raw.substring(0, 157) + "..." : raw;
    }

    private NotificationPriority resolvePriority(String priority) {
        try {
            return NotificationPriority.valueOf(priority != null ? priority.toUpperCase() : "NORMAL");
        } catch (IllegalArgumentException e) {
            return NotificationPriority.NORMAL;
        }
    }

    private boolean isDndActive() {
        int hour = LocalTime.now().getHour();
        if (dndStartHour > dndEndHour) {
            return hour >= dndStartHour || hour < dndEndHour;
        }
        return hour >= dndStartHour && hour < dndEndHour;
    }

    private boolean isRateLimited(String userId) {
        if (userId == null) return false;
        long count = logRepository.countByUserIdSince(userId, LocalDateTime.now().minusHours(1));
        return count >= maxNotificationsPerHour;
    }

    private static final Map<String, String> TEMPLATE_SUBJECTS = Map.ofEntries(
        Map.entry("account-application-submitted", "Your Account Application Has Been Received"),
        Map.entry("account-application-approved",  "Your Account is Ready — Welcome to BankSphere!"),
        Map.entry("account-application-rejected",  "Update on Your Account Application"),
        Map.entry("account-frozen",                "IMPORTANT: Your Account Has Been Frozen"),
        Map.entry("account-unfrozen",              "Your Account is Active Again"),
        Map.entry("account-closed",                "Your Account Has Been Closed"),
        Map.entry("transaction-success",           "Transaction Successful — BankSphere"),
        Map.entry("loan-approved",                 "Congratulations! Your Loan Has Been Approved"),
        Map.entry("loan-rejected",                 "Update on Your Loan Application"),
        Map.entry("password-reset",                "Your BankSphere Password Reset OTP"),
        Map.entry("emi-reminder",                  "EMI Payment Reminder — BankSphere"),
        Map.entry("welcome",                       "Welcome to BankSphere — Your Account is Ready"),
        Map.entry("employee-welcome",              "Welcome to BankSphere — Employee Onboarding")
    );
}

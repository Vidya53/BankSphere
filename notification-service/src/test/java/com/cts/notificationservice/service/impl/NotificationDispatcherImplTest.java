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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link NotificationDispatcherImpl}.
 *
 * Strategy for the DND time-of-day check:
 *   The implementation calls {@code LocalTime.now()} directly, so to keep
 *   tests deterministic regardless of wall-clock time we mostly use
 *   {@code priority = "HIGH"} (which bypasses DND). The dedicated DND-skip
 *   test sets dndStart=0 / dndEnd=24 so the window is always active.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationDispatcherImpl — dispatch & retry")
class NotificationDispatcherImplTest {

    @Mock private NotificationLogRepository logRepository;
    @Mock private EmailTemplateService templateService;
    @Mock private EmailProvider emailProvider;
    @Mock private SmsProvider smsProvider;
    @Mock private PushProvider pushProvider;

    @InjectMocks private NotificationDispatcherImpl dispatcher;

    @BeforeEach
    void setup() {
        // Defaults — match the application.yml defaults
        ReflectionTestUtils.setField(dispatcher, "maxNotificationsPerHour", 20);
        ReflectionTestUtils.setField(dispatcher, "dndStartHour", 22);
        ReflectionTestUtils.setField(dispatcher, "dndEndHour", 8);
        ReflectionTestUtils.setField(dispatcher, "maxRetryAttempts", 3);
        // Template service returns the body unchanged
        when(templateService.render(anyString(), any())).thenReturn("<html>body</html>");
        when(templateService.renderGeneric(any(), any(), any())).thenReturn("<html>generic</html>");
        when(templateService.renderSms(anyString(), any())).thenReturn("SMS body");
    }

    private NotificationEventMessage message(List<String> channels, String priority) {
        return new NotificationEventMessage(
                "NOTIF-001", "REQ-001", "USR1",
                "user@example.com", "9876543210",
                channels, "welcome", Map.of("name", "Alice"),
                "Welcome", "Welcome body", priority,
                "account-service", "en", LocalDateTime.now());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  dispatch
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("dispatch(...)")
    class Dispatch {

        @Test
        @DisplayName("EMAIL channel happy path — saves SENT log and calls EmailProvider")
        void emailHappy() throws Exception {
            NotificationEventMessage msg = message(List.of("EMAIL"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(emailProvider).send(eq("user@example.com"), anyString(), anyString());
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository, atLeastOnce()).save(captor.capture());
            NotificationLog last = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(last.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(last.getChannel()).isEqualTo(NotificationType.EMAIL);
        }

        @Test
        @DisplayName("SMS channel happy path — calls SmsProvider")
        void smsHappy() throws Exception {
            NotificationEventMessage msg = message(List.of("SMS"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(smsProvider).send(eq("9876543210"), anyString());
        }

        @Test
        @DisplayName("PUSH channel happy path — calls PushProvider")
        void pushHappy() throws Exception {
            NotificationEventMessage msg = message(List.of("PUSH"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(pushProvider).send(eq("USR1"), anyString(), anyString());
        }

        @Test
        @DisplayName("multi-channel — one message produces a log entry per channel")
        void multiChannel() throws Exception {
            NotificationEventMessage msg = message(List.of("EMAIL", "SMS", "PUSH"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(emailProvider).send(anyString(), anyString(), anyString());
            verify(smsProvider).send(anyString(), anyString());
            verify(pushProvider).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("idempotency — skips when SENT log already exists for that (id, channel)")
        void idempotencySkip() throws Exception {
            NotificationEventMessage msg = message(List.of("EMAIL"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(
                    eq("NOTIF-001"), eq(NotificationType.EMAIL), eq(NotificationStatus.SENT)))
                    .thenReturn(true);

            dispatcher.dispatch(msg);

            verify(emailProvider, never()).send(any(), any(), any());
            verify(logRepository, never()).save(any());
        }

        @Test
        @DisplayName("DND skip — NORMAL priority during full-day DND window writes SKIPPED log")
        void dndSkip() throws Exception {
            ReflectionTestUtils.setField(dispatcher, "dndStartHour", 0);
            ReflectionTestUtils.setField(dispatcher, "dndEndHour", 24);
            NotificationEventMessage msg = message(List.of("EMAIL"), "NORMAL");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(emailProvider, never()).send(any(), any(), any());
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
            assertThat(captor.getValue().getErrorMessage()).contains("DND");
        }

        @Test
        @DisplayName("rate limit — LOW priority over hourly cap writes SKIPPED log")
        void rateLimitSkip() throws Exception {
            // Disable DND so we hit the rate-limit branch directly.
            ReflectionTestUtils.setField(dispatcher, "dndStartHour", 0);
            ReflectionTestUtils.setField(dispatcher, "dndEndHour", 0);
            NotificationEventMessage msg = message(List.of("EMAIL"), "LOW");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.countByUserIdSince(eq("USR1"), any())).thenReturn(999L);
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.dispatch(msg);

            verify(emailProvider, never()).send(any(), any(), any());
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
            assertThat(captor.getValue().getErrorMessage()).contains("Rate limit");
        }

        @Test
        @DisplayName("provider failure — increments retryCount and marks FAILED")
        void providerFailureMarksFailed() throws Exception {
            NotificationEventMessage msg = message(List.of("EMAIL"), "HIGH");
            when(logRepository.existsByNotificationIdAndChannelAndStatus(any(), any(), any()))
                    .thenReturn(false);
            when(logRepository.findByNotificationIdAndChannel(any(), any())).thenReturn(Optional.empty());
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("smtp down"))
                    .when(emailProvider).send(anyString(), anyString(), anyString());

            dispatcher.dispatch(msg);

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(logRepository, atLeastOnce()).save(captor.capture());
            NotificationLog last = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(last.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(last.getRetryCount()).isEqualTo(1);
            assertThat(last.getErrorMessage()).contains("smtp");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  retryFailed
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("retryFailed(...)")
    class RetryFailed {

        @Test
        @DisplayName("redelivers a FAILED entry successfully — sets SENT, clears error")
        void redeliverSuccess() throws Exception {
            NotificationLog entry = NotificationLog.builder()
                    .notificationId("NOTIF-1").userId("USR1")
                    .channel(NotificationType.EMAIL)
                    .recipientEmail("u@e.com").subject("s").body("b")
                    .status(NotificationStatus.FAILED).priority(NotificationPriority.NORMAL)
                    .retryCount(1).build();
            when(logRepository.findRetryable(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(entry));
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<NotificationLog> redelivered = dispatcher.retryFailed(10);

            assertThat(redelivered).hasSize(1);
            verify(emailProvider).send(eq("u@e.com"), eq("s"), eq("b"));
            assertThat(entry.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(entry.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("after maxRetries is hit on failure, entry is marked DLT")
        void escalateToDlt() throws Exception {
            NotificationLog entry = NotificationLog.builder()
                    .notificationId("NOTIF-2").userId("USR1")
                    .channel(NotificationType.EMAIL)
                    .recipientEmail("u@e.com").subject("s").body("b")
                    .status(NotificationStatus.FAILED).priority(NotificationPriority.NORMAL)
                    .retryCount(2) // next attempt -> 3, equals maxRetryAttempts
                    .build();
            when(logRepository.findRetryable(anyInt(), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(entry));
            when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("still down"))
                    .when(emailProvider).send(anyString(), anyString(), anyString());

            dispatcher.retryFailed(10);

            assertThat(entry.getStatus()).isEqualTo(NotificationStatus.DLT);
            assertThat(entry.getRetryCount()).isEqualTo(3);
            verify(logRepository, times(1)).save(entry);
        }
    }
}

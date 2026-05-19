package com.cts.notificationservice.controller;

import com.cts.notificationservice.dto.response.NotificationLogResponse;
import com.cts.notificationservice.entity.NotificationLog;
import com.cts.notificationservice.enums.NotificationStatus;
import com.cts.notificationservice.repository.NotificationLogRepository;
import com.cts.notificationservice.service.NotificationDispatcher;
import com.cts.notificationservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Notification Log", description = "Query and manage notification delivery history")
public class NotificationLogController {

    private final NotificationLogRepository logRepository;
    private final NotificationDispatcher dispatcher;

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get notification history for a user",
            description = """
                    Returns a paginated history of all notifications dispatched to the given user across every channel.
                    Idempotency key `(notificationId, channel, SENT)` ensures duplicate dispatches for the same logical event are not double-counted.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> getByUser(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<NotificationLogResponse> page = logRepository.findAllByUserId(userId, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(page,
                "Notification history for user " + userId));
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Get notifications by delivery status",
            description = """
                    Lists notifications filtered by delivery status (SENT, FAILED, PENDING, etc.) for ops monitoring.
                    Useful for triaging FAILED entries that are awaiting the 5-minute retry sweep or have hit `notification.retry.max-attempts` and been routed to `banking.notification.events-dlt`.

                    **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> getByStatus(
            @PathVariable NotificationStatus status,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        Page<NotificationLogResponse> page = logRepository.findAllByStatus(status, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(page,
                status + " notifications — total: " + page.getTotalElements()));
    }

    @PostMapping("/retry")
    @Operation(
            summary = "Manually trigger retry for FAILED notifications",
            description = """
                    Forces an immediate retry sweep over the oldest FAILED notifications (up to `batchSize`), bypassing the 5-minute scheduled retry. Intended for ops use only.

                    **Allowed roles:** ADMIN
                    **Side effects:** Re-dispatches via the notification dispatcher (email/SMS/push providers) and writes new attempt rows to the notification log."""
    )
    public ResponseEntity<ApiResponse<Integer>> triggerRetry(
            @RequestParam(defaultValue = "100") int batchSize) {

        List<NotificationLog> retried = dispatcher.retryFailed(batchSize);
        return ResponseEntity.ok(ApiResponse.success(retried.size(),
                "Retry triggered for " + retried.size() + " notifications"));
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .id(log.getId())
                .notificationId(log.getNotificationId())
                .userId(log.getUserId())
                .recipientEmail(log.getRecipientEmail())
                .recipientPhone(log.getRecipientPhone())
                .channel(log.getChannel())
                .templateId(log.getTemplateId())
                .subject(log.getSubject())
                .status(log.getStatus())
                .priority(log.getPriority())
                .serviceName(log.getServiceName())
                .retryCount(log.getRetryCount())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

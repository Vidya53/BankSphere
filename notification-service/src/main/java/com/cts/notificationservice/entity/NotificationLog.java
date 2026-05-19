package com.cts.notificationservice.entity;

import com.cts.notificationservice.enums.NotificationPriority;
import com.cts.notificationservice.enums.NotificationStatus;
import com.cts.notificationservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notification_logs",
    indexes = {
        @Index(name = "idx_notif_notification_id", columnList = "notification_id"),
        @Index(name = "idx_notif_user_id",         columnList = "user_id"),
        @Index(name = "idx_notif_status",          columnList = "status"),
        @Index(name = "idx_notif_channel_status",  columnList = "channel, status"),
        @Index(name = "idx_notif_created_at",      columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, length = 36)
    private String notificationId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationType channel;

    @Column(name = "template_id", length = 80)
    private String templateId;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private NotificationPriority priority;

    @Column(name = "service_name", length = 50)
    private String serviceName;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

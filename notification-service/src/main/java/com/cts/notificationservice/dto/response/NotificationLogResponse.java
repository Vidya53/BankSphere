package com.cts.notificationservice.dto.response;

import com.cts.notificationservice.enums.NotificationPriority;
import com.cts.notificationservice.enums.NotificationStatus;
import com.cts.notificationservice.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationLogResponse {

    private Long id;
    private String notificationId;
    private String userId;
    private String recipientEmail;
    private String recipientPhone;
    private NotificationType channel;
    private String templateId;
    private String subject;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String serviceName;
    private int retryCount;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

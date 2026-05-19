package com.cts.auditservice.dto.response;

import com.cts.auditservice.enums.AuditStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private String eventId;
    private String requestId;
    private String serviceName;
    private String action;
    private String entityType;
    private String entityId;
    private String performedBy;
    private String userRole;
    private AuditStatus status;
    private String branchCode;
    private String ipAddress;
    private String userAgent;
    private String details;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

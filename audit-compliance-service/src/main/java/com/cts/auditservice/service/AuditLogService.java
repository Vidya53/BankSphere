package com.cts.auditservice.service;

import com.cts.auditservice.dto.AuditEventMessage;
import com.cts.auditservice.dto.response.AuditLogResponse;
import com.cts.auditservice.dto.response.AuditSummaryResponse;
import com.cts.auditservice.enums.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    // Ingestion — called by consumer and REST endpoint
    void ingest(AuditEventMessage message);

    // Compliance queries
    Page<AuditLogResponse> queryLogs(
            String serviceName, String action, String performedBy,
            AuditStatus status, String entityType, String entityId,
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    AuditLogResponse getById(Long id);

    Page<AuditLogResponse> getByEntity(String entityType, String entityId, Pageable pageable);

    Page<AuditLogResponse> getByUser(String performedBy, Pageable pageable);

    AuditSummaryResponse getSummary(LocalDateTime from, LocalDateTime to);
}

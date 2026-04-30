package com.cts.accountservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock audit service.
 * In production, replaced by WebClient call to audit-service.
 */
@Service
@Slf4j
public class AuditService {

    /**
     * TODO: Replace with WebClient call to audit-service: POST /api/v1/internal/audit
     */
    public void logAudit(String userId, String role, String action, String entityType, String entityId, String branchCode) {
        log.info("[AUDIT] User: {}, Role: {}, Action: {}, Entity: {}:{}, Branch: {}",
                userId, role, action, entityType, entityId, branchCode);
    }
}


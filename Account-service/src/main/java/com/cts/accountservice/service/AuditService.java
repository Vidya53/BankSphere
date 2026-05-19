package com.cts.accountservice.service;

public interface AuditService {

    void logAudit(String userId, String role, String action,
                  String entityType, String entityId, String branchCode);
}

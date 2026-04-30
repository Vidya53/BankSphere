package com.cts.accountservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock branch validation service.
 * In production, replaced by WebClient call to branch-service.
 */
@Service
@Slf4j
public class BranchValidationService {

    /**
     * Check if branch exists and is active.
     * TODO: Replace with WebClient call to branch-service: GET /api/v1/internal/branches/{branchCode}/validate
     */
    public boolean isBranchActive(String branchCode) {
        log.info("[MOCK] Branch validation for {}: returning ACTIVE", branchCode);
        return true;
    }

    /**
     * Get IFSC code for a branch.
     * TODO: Replace with WebClient call to branch-service
     */
    public String getIfscCode(String branchCode) {
        log.info("[MOCK] IFSC lookup for branch {}", branchCode);
        return "BNKS0" + branchCode;
    }
}


package com.cts.accountservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock KYC verification service.
 * In production, this will be replaced by a WebClient call to compliance-service.
 * Currently returns true for all customers to enable testing.
 */
@Service
@Slf4j
public class KycVerificationService {

    /**
     * Check if customer KYC is approved.
     * TODO: Replace with WebClient call to compliance-service: GET /api/v1/internal/kyc/status/{customerId}
     */
    public boolean isKycApproved(String customerId) {
        log.info("[MOCK] KYC check for customer {}: returning APPROVED", customerId);
        return true;
    }
}


package com.cts.accountservice.service;

import com.cts.accountservice.client.CustomerServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycVerificationService {

    private final CustomerServiceClient customerServiceClient;

    public boolean isKycApproved(String userId) {
        try {
            Boolean approved = customerServiceClient.isKycApprovedByUserId(userId);
            boolean result = Boolean.TRUE.equals(approved);
            log.info("KYC check for userId {}: {}", userId, result ? "APPROVED" : "NOT APPROVED");
            return result;
        } catch (Exception e) {
            log.error("KYC check failed for userId {}: {}", userId, e.getMessage());
            return false;
        }
    }
}

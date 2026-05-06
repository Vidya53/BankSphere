package com.cts.accountservice.service;

import com.cts.accountservice.client.CustomerServiceClient;
import com.cts.accountservice.client.dto.KycApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycVerificationService {

    private final CustomerServiceClient customerServiceClient;

    public boolean isKycApproved(String customerId) {
        try {
            KycApiResponse response = customerServiceClient.getKycStatus(customerId);
            if (response == null || response.getData() == null) {
                log.warn("KYC response empty for customer {} — treating as NOT approved", customerId);
                return false;
            }
            boolean approved = "APPROVED".equalsIgnoreCase(response.getData().getStatus());
            log.info("KYC check for customer {}: {}", customerId, response.getData().getStatus());
            return approved;
        } catch (Exception e) {
            log.error("KYC check failed for customer {}: {}", customerId, e.getMessage());
            return false;
        }
    }
}

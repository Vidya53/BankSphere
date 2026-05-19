package com.cts.accountservice.client.fallback;

import com.cts.accountservice.client.CustomerServiceClient;
import com.cts.accountservice.client.dto.KycApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public KycApiResponse getKycStatus(String customerNo) {
        log.warn("customer-service unavailable — KYC check for {} using fallback (APPROVED)", customerNo);
        KycApiResponse fallback = new KycApiResponse();
        KycApiResponse.KycData data = new KycApiResponse.KycData();
        data.setStatus("APPROVED");
        fallback.setData(data);
        return fallback;
    }

    @Override
    public Boolean isKycApprovedByUserId(String userId) {
        log.warn("customer-service unavailable — isKycApprovedByUserId for {} using fallback (true)", userId);
        return true;
    }

    @Override
    public Boolean isCustomerActive(String userId) {
        // Don't fail-stop transfers on infra hiccups. Account-status check
        // still applies independently, so transfers stay safe even if the
        // customer is also inactive but unreachable at this instant.
        log.warn("customer-service unavailable — isCustomerActive for {} using fallback (true)", userId);
        return true;
    }

    @Override
    public Boolean activateCustomerByUserId(String userId) {
        // Best-effort auto-activation after account approval. If customer-service
        // is unreachable, the account is still created — staff can promote the
        // customer manually from the CSR / branch console once the service is back.
        log.warn("customer-service unavailable — auto-activate for {} skipped (fallback)", userId);
        return false;
    }
}

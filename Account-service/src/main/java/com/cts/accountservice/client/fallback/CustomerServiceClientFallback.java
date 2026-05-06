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
}

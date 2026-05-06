package com.cts.accountservice.client;

import com.cts.accountservice.client.dto.KycApiResponse;
import com.cts.accountservice.client.fallback.CustomerServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", fallback = CustomerServiceClientFallback.class)
public interface CustomerServiceClient {

    @GetMapping("/customers/{customerNo}/kyc")
    KycApiResponse getKycStatus(@PathVariable("customerNo") String customerNo);
}

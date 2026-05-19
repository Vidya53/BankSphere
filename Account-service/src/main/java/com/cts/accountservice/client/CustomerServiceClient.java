package com.cts.accountservice.client;

import com.cts.accountservice.client.dto.KycApiResponse;
import com.cts.accountservice.client.fallback.CustomerServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "customer-service", fallback = CustomerServiceClientFallback.class)
public interface CustomerServiceClient {

    @GetMapping("/customers/{customerNo}/kyc")
    KycApiResponse getKycStatus(@PathVariable("customerNo") String customerNo);

    @GetMapping("/api/v1/internal/customers/{userId}/kyc-approved")
    Boolean isKycApprovedByUserId(@PathVariable("userId") String userId);

    @GetMapping("/api/v1/internal/customers/{userId}/active")
    Boolean isCustomerActive(@PathVariable("userId") String userId);

    @PostMapping("/api/v1/internal/customers/{userId}/activate")
    Boolean activateCustomerByUserId(@PathVariable("userId") String userId);
}

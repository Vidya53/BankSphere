package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.CustomerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Feign Client for Customer Service
 * Handles remote calls to customer-service internal endpoints
 */
@FeignClient(name = "customer-service")
public interface CustomerServiceClient extends CustomerClient {

    @PostMapping("/api/v1/customers/internal/check-eligibility")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "checkEligibilityFallback")
    @Retry(name = "customer-service")
    boolean checkLoanEligibility(@RequestBody EligibilityRequest request);

    @GetMapping("/api/v1/customers/internal/{customerNo}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customer-service")
    CustomerDTO getCustomer(@PathVariable String customerNo);

    /**
     * Override the interface method with actual HTTP call
     */
    @Override
    @PostMapping("/api/v1/customers/internal/check-eligibility")
    default boolean isEligible(String customerId) {
        return checkLoanEligibility(new EligibilityRequest(customerId));
    }

    /**
     * Fallback for checkEligibility when circuit breaker is open
     */
    default boolean checkEligibilityFallback(EligibilityRequest request, Exception e) {
        throw new RuntimeException("Customer service unavailable. Cannot check eligibility for customer: " + request.getCustomerId(), e);
    }

    /**
     * Fallback for getCustomer when circuit breaker is open
     */
    default CustomerDTO getCustomerFallback(String customerNo, Exception e) {
        throw new RuntimeException("Customer service unavailable. Cannot fetch customer: " + customerNo, e);
    }

    @Data
    @AllArgsConstructor
    class EligibilityRequest {
        private String customerId;
        private int minAge = 21;
        private int maxAge = 65;
    }

    @Data
    class CustomerDTO {
        private String customerNo;
        private String name;
        private int age;
        private String status;
        private String kycStatus;
    }
}



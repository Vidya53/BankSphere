package com.cts.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;

/**
 * Feign Client for Customer Service
 * Used by Account Service to retrieve customer information
 */
@FeignClient(name = "customer-service")
public interface CustomerServiceClient {

    /**
     * Get customer details by customer number - Internal endpoint
     */
    @GetMapping("/api/v1/customers/internal/{customerNo}")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customer-service")
    CustomerDTO getCustomer(@PathVariable String customerNo);

    /**
     * Check if customer is active - Internal endpoint
     */
    @GetMapping("/api/v1/customers/internal/{customerNo}/status")
    @CircuitBreaker(name = "customer-service", fallbackMethod = "checkStatusFallback")
    @Retry(name = "customer-service")
    boolean isCustomerActive(@PathVariable String customerNo);

    /**
     * Fallback for getCustomer
     */
    default CustomerDTO getCustomerFallback(String customerNo, Exception e) {
        throw new RuntimeException("Customer service unavailable. Cannot fetch customer: " + customerNo, e);
    }

    /**
     * Fallback for checkStatus
     */
    default boolean checkStatusFallback(String customerNo, Exception e) {
        throw new RuntimeException("Customer service unavailable. Cannot verify customer status: " + customerNo, e);
    }

    @Data
    class CustomerDTO {
        private String customerNo;
        private String name;
        private String email;
        private String phoneNumber;
        private String status;
        private int age;
        private String kycStatus;
    }
}



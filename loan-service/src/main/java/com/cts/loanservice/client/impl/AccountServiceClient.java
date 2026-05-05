package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.AccountClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Feign Client for Account Service
 * Handles inter-service calls for account operations (debit/credit)
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient extends AccountClient {

    /**
     * Credit amount to account - Internal API
     */
    @PostMapping("/api/v1/internal/accounts/{accountNo}/credit")
    @CircuitBreaker(name = "account-service", fallbackMethod = "creditFallback")
    @Retry(name = "account-service")
    void creditInternal(
            @PathVariable String accountNo,
            @RequestParam Double amount
    );

    /**
     * Debit amount from account - Internal API
     */
    @PostMapping("/api/v1/internal/accounts/{accountNo}/debit")
    @CircuitBreaker(name = "account-service", fallbackMethod = "debitFallback")
    @Retry(name = "account-service")
    void debitInternal(
            @PathVariable String accountNo,
            @RequestParam Double amount
    );

    /**
     * Override interface method with actual HTTP call
     */
    @Override
    default void credit(String accountId, Double amount) {
        creditInternal(accountId, amount);
    }

    /**
     * Override interface method with actual HTTP call
     */
    @Override
    default void debit(String accountId, Double amount) {
        debitInternal(accountId, amount);
    }

    /**
     * Fallback method for credit operation when circuit breaker is open
     */
    default void creditFallback(String accountNo, Double amount, Exception e) {
        throw new RuntimeException("Account service is unavailable. Credit operation failed for account: " + accountNo, e);
    }

    /**
     * Fallback method for debit operation when circuit breaker is open
     */
    default void debitFallback(String accountNo, Double amount, Exception e) {
        throw new RuntimeException("Account service is unavailable. Debit operation failed for account: " + accountNo, e);
    }
}



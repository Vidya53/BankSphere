package com.cts.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Feign Client for Account Service
 * Used by Transaction Service to validate accounts and retrieve account information
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient {

    /**
     * Get account details by account number - Internal endpoint
     */
    @GetMapping("/api/v1/internal/accounts/{accountNo}")
    @CircuitBreaker(name = "account-service", fallbackMethod = "getAccountFallback")
    @Retry(name = "account-service")
    AccountDTO getAccount(@PathVariable String accountNo);

    /**
     * Check if account is active - Internal endpoint
     */
    @GetMapping("/api/v1/internal/accounts/{accountNo}/active")
    @CircuitBreaker(name = "account-service", fallbackMethod = "isActiveFallback")
    @Retry(name = "account-service")
    boolean isAccountActive(@PathVariable String accountNo);

    /**
     * Get account balance - Internal endpoint
     */
    @GetMapping("/api/v1/internal/accounts/{accountNo}/balance")
    @CircuitBreaker(name = "account-service", fallbackMethod = "getBalanceFallback")
    @Retry(name = "account-service")
    BigDecimal getBalance(@PathVariable String accountNo);

    /**
     * Fallback for getAccount when circuit breaker is open
     */
    default AccountDTO getAccountFallback(String accountNo, Exception e) {
        throw new RuntimeException("Account service unavailable. Cannot fetch account: " + accountNo, e);
    }

    /**
     * Fallback for isAccountActive when circuit breaker is open
     */
    default boolean isActiveFallback(String accountNo, Exception e) {
        throw new RuntimeException("Account service unavailable. Cannot check account status: " + accountNo, e);
    }

    /**
     * Fallback for getBalance when circuit breaker is open
     */
    default BigDecimal getBalanceFallback(String accountNo, Exception e) {
        throw new RuntimeException("Account service unavailable. Cannot fetch balance for account: " + accountNo, e);
    }

    @Data
    class AccountDTO {
        private String accountNo;
        private String customerId;
        private String accountType;
        private BigDecimal balance;
        private String status;
        private String currency;
    }
}



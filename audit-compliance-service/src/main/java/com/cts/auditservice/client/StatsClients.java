package com.cts.auditservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Feign clients used by AnalyticsService to call internal stats endpoints of
 * customer-service, account-service, loan-service and transaction-service.
 *
 * AnalyticsService wraps each call in try/catch so a downstream outage
 * degrades a single tab to empty values rather than 500-ing the entire dashboard.
 */
@Configuration
public class StatsClients {

    @FeignClient(name = "customer-service", contextId = "customerStats")
    public interface CustomerStatsClient {
        @GetMapping("/api/v1/internal/stats/customers")
        Map<String, Object> customerStats();
    }

    @FeignClient(name = "account-service", contextId = "accountStats")
    public interface AccountStatsClient {
        @GetMapping("/api/v1/internal/stats/accounts")
        Map<String, Object> accountStats();
    }

    @FeignClient(name = "loan-service", contextId = "loanStats")
    public interface LoanStatsClient {
        @GetMapping("/api/v1/internal/stats/loans")
        Map<String, Object> loanStats();
    }

    @FeignClient(name = "transaction-service", contextId = "transactionStats")
    public interface TransactionStatsClient {
        @GetMapping("/api/v1/internal/stats/transactions")
        Map<String, Object> transactionStats();
    }
}

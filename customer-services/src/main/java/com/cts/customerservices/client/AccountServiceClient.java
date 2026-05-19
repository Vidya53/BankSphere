package com.cts.customerservices.client;

import com.cts.customerservices.client.fallback.AccountServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Calls account-service's internal endpoints. Today it's only used by the
 * customer soft-delete cascade — when admin/BM deletes a customer, every
 * account that customer owns is closed atomically.
 */
@FeignClient(
        name = "account-service",
        fallback = AccountServiceClientFallback.class
)
public interface AccountServiceClient {

    @PostMapping("/api/v1/internal/accounts/customer/{customerId}/close-all")
    Integer closeAllAccountsForCustomer(@PathVariable("customerId") String customerId,
                                        @RequestParam("reason")   String reason,
                                        @RequestParam("closedBy") String closedBy);
}

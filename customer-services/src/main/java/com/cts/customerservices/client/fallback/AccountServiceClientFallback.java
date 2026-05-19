package com.cts.customerservices.client.fallback;

import com.cts.customerservices.client.AccountServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * If account-service is unreachable during a soft-delete cascade we don't want
 * to block the customer-service operation — the customer row is still flipped
 * to isDeleted=true. The accounts will be reconciled when ops re-runs the
 * close-all call, or by the standard nightly sweep.
 */
@Component
@Slf4j
public class AccountServiceClientFallback implements AccountServiceClient {

    @Override
    public Integer closeAllAccountsForCustomer(String customerId, String reason, String closedBy) {
        log.warn("account-service unavailable during soft-delete cascade for customerId={} — " +
                 "customer was soft-deleted but downstream account closure did not run. " +
                 "Operator must reconcile manually.", customerId);
        return 0;
    }
}

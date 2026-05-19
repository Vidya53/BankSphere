package com.cts.loanservice.client;

import com.cts.loanservice.client.dto.TransactionRecordRequest;
import com.cts.loanservice.client.fallback.TransactionServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Records loan-driven ledger entries (disbursements, EMI payments, prepayments)
 * against transaction-service so they show up in the customer's transaction
 * history alongside transfers and cash-counter operations.
 *
 * The call is best-effort — on failure the loan operation still succeeds and
 * a warning is logged. Reconciling the missing ledger entry is a manual job.
 */
@FeignClient(name = "transaction-service", fallback = TransactionServiceClientFallback.class)
public interface TransactionServiceClient {

    @PostMapping("/api/v1/transactions")
    void initiate(@RequestBody TransactionRecordRequest body);
}

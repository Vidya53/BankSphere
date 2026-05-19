package com.cts.loanservice.client.fallback;

import com.cts.loanservice.client.TransactionServiceClient;
import com.cts.loanservice.client.dto.TransactionRecordRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for {@link TransactionServiceClient}. If transaction-service is
 * down we don't want to roll back the loan operation that already ran — log
 * the gap and move on. Future work: outbox pattern + reconciliation job.
 */
@Component
@Slf4j
public class TransactionServiceClientFallback implements TransactionServiceClient {

    @Override
    public void initiate(TransactionRecordRequest body) {
        log.warn("transaction-service unavailable — ledger entry skipped " +
                "(type={}, idempotencyKey={}, amount={}). The funds movement " +
                "completed on account-service; reconcile this manually.",
                body == null ? "?" : body.getTransactionType(),
                body == null ? "?" : body.getIdempotencyKey(),
                body == null ? "?" : body.getAmount());
    }
}

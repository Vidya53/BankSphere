package com.cts.accountservice.client.fallback;

import com.cts.accountservice.client.TransactionServiceClient;
import com.cts.accountservice.client.dto.TransactionRecordRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class TransactionServiceClientFallback implements TransactionServiceClient {

    @Override
    public Map<String, Object> initiate(TransactionRecordRequest request) {
        log.warn("transaction-service unavailable — could not record txn idempotencyKey={}",
                request.getIdempotencyKey());
        return Map.of("data", Map.of("transactionId", "OFFLINE-" + System.currentTimeMillis(),
                "referenceNumber", "OFFLINE", "status", "PENDING"));
    }

    @Override
    public Map<String, Object> markSuccess(String transactionId, BigDecimal senderBalance, BigDecimal receiverBalance) {
        log.warn("transaction-service unavailable — could not mark {} SUCCESS", transactionId);
        return Map.of("data", Map.of("transactionId", transactionId, "status", "SUCCESS"));
    }

    @Override
    public Map<String, Object> markFailed(String transactionId, String failureReason) {
        log.warn("transaction-service unavailable — could not mark {} FAILED ({})", transactionId, failureReason);
        return Map.of("data", Map.of("transactionId", transactionId, "status", "FAILED"));
    }
}

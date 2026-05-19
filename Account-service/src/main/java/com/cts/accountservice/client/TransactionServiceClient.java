package com.cts.accountservice.client;

import com.cts.accountservice.client.dto.TransactionRecordRequest;
import com.cts.accountservice.client.fallback.TransactionServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feign client used by account-service to record money-movement events
 * into the transaction-service ledger. Calls are best-effort: a failed
 * ledger write must not block a real funds transfer (the fallback returns
 * a synthetic ack so the account-side flow continues).
 */
@FeignClient(name = "transaction-service", fallback = TransactionServiceClientFallback.class)
public interface TransactionServiceClient {

    @PostMapping("/api/v1/transactions")
    Map<String, Object> initiate(@RequestBody TransactionRecordRequest request);

    @PatchMapping("/api/v1/transactions/{transactionId}/success")
    Map<String, Object> markSuccess(@PathVariable("transactionId") String transactionId,
                                    @RequestParam("senderBalance")   BigDecimal senderBalance,
                                    @RequestParam("receiverBalance") BigDecimal receiverBalance);

    @PatchMapping("/api/v1/transactions/{transactionId}/fail")
    Map<String, Object> markFailed(@PathVariable("transactionId") String transactionId,
                                   @RequestParam("failureReason")  String failureReason);
}

package com.cts.loanservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload sent to transaction-service's POST /api/v1/transactions.
 * Field names mirror its TransactionRequestDto exactly so Jackson maps
 * cleanly without aliases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecordRequest {

    /** Account being debited. Null for DEPOSIT-type entries (disbursements). */
    private String     senderAccountId;

    /** Account being credited. Null for PAYMENT/WITHDRAWAL entries. */
    private String     receiverAccountId;

    private BigDecimal amount;

    /** ISO 4217 — we always use INR. */
    private String     currency;

    /** DEPOSIT for disbursement, PAYMENT for EMI / prepayment / foreclosure. */
    private String     transactionType;

    /** INTERNAL for every loan-driven movement. */
    private String     channel;

    /** Must be unique. We reuse the loan / EMI reference so the call is safe to retry. */
    private String     idempotencyKey;

    private String     description;

    /** SUCCESS — the underlying account-service call has already moved the funds. */
    private String     initialStatus;
}

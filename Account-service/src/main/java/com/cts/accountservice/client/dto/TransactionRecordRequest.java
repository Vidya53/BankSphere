package com.cts.accountservice.client.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payload sent to transaction-service's POST /api/v1/transactions endpoint.
 * Field names mirror its TransactionRequestDto exactly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecordRequest {

    @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Sender account number must be a 3-letter prefix followed by 14 alphanumerics")
    private String     senderAccountId;

    @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Receiver account number must be a 3-letter prefix followed by 14 alphanumerics")
    private String     receiverAccountId;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount supports up to 13 integer and 2 fractional digits")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. INR, USD)")
    private String     currency;          // INR

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEPOSIT|WITHDRAWAL|TRANSFER|PAYMENT|REFUND|REVERSAL|FEE|TAX|INTEREST|CHARGE)$", message = "Transaction type must be one of DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND, REVERSAL, FEE, TAX, INTEREST or CHARGE")
    private String     transactionType;   // DEPOSIT / WITHDRAWAL / TRANSFER / PAYMENT

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "^(UPI|NEFT|IMPS|RTGS|INTERNAL|BRANCH|ATM|MOBILE_APP|NET_BANKING|API|CASH)$", message = "Channel must be one of UPI, NEFT, IMPS, RTGS, INTERNAL, BRANCH, ATM, MOBILE_APP, NET_BANKING, API or CASH")
    private String     channel;           // UPI / NEFT / IMPS / RTGS / INTERNAL

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String     idempotencyKey;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String     description;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String     remarks;

    /** PENDING (default), SUCCESS or FAILED — lets us record completed funds movements in one call. */
    @Pattern(regexp = "^$|^(PENDING|SUCCESS|FAILED)$", message = "Initial status must be PENDING, SUCCESS or FAILED")
    private String     initialStatus;

    /** Balances after debit/credit; only used when initialStatus = SUCCESS. */
    @DecimalMin(value = "0.00", message = "Sender balance cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Sender balance supports up to 13 integer and 2 fractional digits")
    private BigDecimal senderBalanceAfter;

    @DecimalMin(value = "0.00", message = "Receiver balance cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Receiver balance supports up to 13 integer and 2 fractional digits")
    private BigDecimal receiverBalanceAfter;
}

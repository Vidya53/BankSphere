package com.cts.transactionservice.dto.request;

import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequestDto {
    @Size(min = 5, max = 20, message = "Sender account ID must be between 5 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Sender account ID must contain only uppercase letters and digits")
    private String senderAccountId;

    @Size(min = 5, max = 20, message = "Receiver account ID must be between 5 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Receiver account ID must contain only uppercase letters and digits")
    private String receiverAccountId;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Amount must be greater than zero")
    @DecimalMax(value = "9999999999999.9999", message = "Amount exceeds the maximum allowed limit")
    @Digits(integer = 13, fraction = 4, message = "Amount must have at most 13 integer digits and 4 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. INR, USD)")
    private String currency;

    @DecimalMin(value = "0.0", inclusive = true, message = "Fee cannot be negative")
    @Digits(integer = 6, fraction = 4, message = "Fee must have at most 6 integer digits and 4 decimal places")
    private BigDecimal fee;

    @DecimalMin(value = "0.0", inclusive = true, message = "Tax cannot be negative")
    @Digits(integer = 6, fraction = 4, message = "Tax must have at most 6 integer digits and 4 decimal places")
    private BigDecimal tax;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Transaction channel is required")
    private TransactionChannel channel;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$", message = "Idempotency key may contain only letters, digits, underscores and hyphens")
    private String idempotencyKey;

    @Size(max = 36, message = "Parent transaction ID must not exceed 36 characters")
    @Pattern(regexp = "^$|^[a-fA-F0-9-]{36}$", message = "Parent transaction ID must be a valid 36-character UUID")
    private String parentTransactionId;

    @Size(max = 100, message = "External reference ID must not exceed 100 characters")
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{1,100}$", message = "External reference ID may contain only letters, digits, underscores and hyphens")
    private String externalReferenceId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @Size(max = 255, message = "Device info must not exceed 255 characters")
    private String deviceInfo;

    @Size(max = 50, message = "Geolocation must not exceed 50 characters")
    @Pattern(
            regexp = "^-?([1-8]?[0-9](\\.[0-9]+)?|90(\\.0+)?),\\s*-?(1[0-7][0-9](\\.[0-9]+)?|[0-9]{1,2}(\\.[0-9]+)?|180(\\.0+)?)$",
            message = "Geolocation must be a valid 'lat,long' coordinate"
    )
    private String geolocation;

    /**
     * Optional initial status for service-to-service calls that already finalised
     * the funds movement (e.g. account-service after a successful debit/credit).
     * Defaults to PENDING when not supplied so customer-facing initiation flow
     * is unchanged.
     */
    private TransactionStatus initialStatus;

    /** Sender's balance AFTER the debit — only used when initialStatus = SUCCESS. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Sender balance after debit cannot be negative")
    @Digits(integer = 13, fraction = 4, message = "Sender balance must have at most 13 integer digits and 4 decimal places")
    private BigDecimal senderBalanceAfter;

    /** Receiver's balance AFTER the credit — only used when initialStatus = SUCCESS. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Receiver balance after credit cannot be negative")
    @Digits(integer = 13, fraction = 4, message = "Receiver balance must have at most 13 integer digits and 4 decimal places")
    private BigDecimal receiverBalanceAfter;
}

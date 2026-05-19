package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmiPaymentRequest {

    @NotBlank(message = "Account ID is required")
    @Pattern(regexp = "^[A-Z]{3}[A-Z0-9]{14}$", message = "Account number must be a 3-letter prefix followed by 14 alphanumerics")
    private String accountId;

    @NotNull(message = "Payment amount is required")
    @Positive
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits and 2 decimal places")
    private Double amount;

    /**
     * The customer's transaction PIN. Verified by account-service before
     * any debit happens — same flow as transfer.
     */
    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits")
    private String pin;
}

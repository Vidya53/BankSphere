package com.cts.accountservice.dto.request;

import com.cts.accountservice.enums.AccountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApplicationRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Invalid deposit amount format")
    private BigDecimal initialDeposit;

    @Size(max = 100, message = "Nominee name must not exceed 100 characters")
    private String nomineeName;

    @Size(max = 50, message = "Nominee relation must not exceed 50 characters")
    private String nomineeRelation;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid nominee phone number")
    private String nomineePhone;

    @Size(max = 200, message = "Nominee address must not exceed 200 characters")
    private String nomineeAddress;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;
}


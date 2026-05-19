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

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "Branch code must be 2-20 uppercase letters or digits")
    private String branchCode;

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative; please enter zero or a positive amount")
    @Digits(integer = 13, fraction = 2, message = "Amount supports up to 13 integer and 2 fractional digits")
    private BigDecimal initialDeposit;

    @Size(min = 2, max = 100, message = "Nominee name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,99}$", message = "Nominee name may contain only letters, spaces, hyphens and apostrophes")
    private String nomineeName;

    @Size(min = 2, max = 50, message = "Nominee relation must be between 2 and 50 characters")
    private String nomineeRelation;

    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "Nominee mobile must be a 10-digit number starting with 6, 7, 8 or 9")
    private String nomineePhone;

    @Size(max = 200, message = "Nominee address must not exceed 200 characters")
    private String nomineeAddress;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;
}

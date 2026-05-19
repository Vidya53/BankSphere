package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDecisionRequest {

    @NotBlank(message = "Decision status is required")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Decision must be APPROVED or REJECTED")
    private String status;

    @Positive
    @DecimalMin(value = "0.1", message = "Interest rate must be at least 0.1% per annum")
    @DecimalMax(value = "30.0", message = "Interest rate cannot exceed 30% per annum")
    @Digits(integer = 2, fraction = 4, message = "Interest rate must have at most 2 digits and 4 decimal places")
    private Double interestRate;
}

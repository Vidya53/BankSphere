package com.cts.customerservices.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoanApplicationRequest {
    @NotBlank(message = "Customer number is required")
    private String customerNo;

    @NotNull(message = "Requested amount is required")
    @Min(value = 10000, message = "Minimum loan amount is ₹10,000")
    @Max(value = 100000000, message = "Maximum loan amount is ₹10,00,00,000")
    private Double requestedAmount;

    @NotNull(message = "Repayment duration is required")
    @Min(value = 6, message = "Minimum repayment duration is 6 months")
    @Max(value = 360, message = "Maximum repayment duration is 360 months (30 years)")
    private Integer repayDurationMonths;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String loanPurpose;
}
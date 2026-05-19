package com.cts.customerservices.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoanApplicationRequest {
    @NotBlank(message = "Customer number is required")
    @Pattern(regexp = "^CUST-[A-Z0-9]{8}$", message = "Customer number must be in the format CUST-XXXXXXXX")
    private String customerNo;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "10000.00", message = "Minimum loan amount is 10,000")
    @DecimalMax(value = "100000000.00", message = "Maximum loan amount is 10,00,00,000")
    private Double requestedAmount;

    @NotNull(message = "Repayment duration is required")
    @Min(value = 6, message = "Minimum repayment duration is 6 months")
    @Max(value = 360, message = "Maximum repayment duration is 360 months (30 years)")
    private Integer repayDurationMonths;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String loanPurpose;
}

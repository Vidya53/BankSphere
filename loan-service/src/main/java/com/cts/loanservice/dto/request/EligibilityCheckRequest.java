package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class EligibilityCheckRequest {

    @NotBlank(message = "Customer ID is required")
    @Pattern(regexp = "^CUST-[A-Z0-9]{8}$", message = "Customer number must look like CUST-XXXXXXXX")
    private String customerId;

    @NotNull(message = "Monthly income is required")
    @Positive
    @DecimalMin(value = "0.01", message = "Monthly income must be greater than zero")
    @DecimalMax(value = "100000000.00", message = "Monthly income looks unrealistically high")
    @Digits(integer = 13, fraction = 2, message = "Monthly income must have at most 13 digits and 2 decimal places")
    private Double monthlyIncome;

    @NotNull(message = "Requested loan amount is required")
    @Positive
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is ₹1,000")
    @DecimalMax(value = "50000000.00", message = "Maximum loan amount is ₹5,00,00,000")
    @Digits(integer = 13, fraction = 2, message = "Loan amount must have at most 13 digits and 2 decimal places")
    private Double requestedAmount;

    @NotNull(message = "Requested tenure (in months) is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    private Integer requestedTenureMonths;
}

package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplyRequest {

    @NotBlank
    private String customerId;

    @NotBlank
    private String accountId;

    @NotBlank
    private String loanType;  // HOME, PERSONAL, CAR, EDUCATION, BUSINESS

    @NotNull
    @Positive
    private Double amount;

    @NotNull
    @Min(1)
    private Integer tenureMonths;

    @NotNull
    @Positive
    private Double income;
}
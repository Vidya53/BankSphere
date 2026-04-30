package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EligibilityCheckRequest {

    @NotBlank
    private String customerId;

    @NotNull
    @Positive
    private Double monthlyIncome;

    @NotNull
    @Positive
    private Double requestedAmount;

    @NotNull
    private Integer requestedTenureMonths;
}


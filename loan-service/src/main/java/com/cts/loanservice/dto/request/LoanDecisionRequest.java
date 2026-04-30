package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LoanDecisionRequest {

    @NotBlank
    private String status;

    @Positive
    private Double interestRate;
}
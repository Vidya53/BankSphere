package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanDecisionRequest {

    @NotBlank
    private String status;

    private Double interestRate;
}
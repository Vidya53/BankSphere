package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanApplyRequest {

    @NotBlank
    private String customerId;

    @NotNull
    private Double amount;

    @NotNull
    private Integer tenureMonths;

    @NotNull
    private Double income;
}
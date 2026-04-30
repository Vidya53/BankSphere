package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EmiPaymentRequest {

    @NotBlank
    private String accountId;

    @NotNull
    @Positive
    private Double amount;
}
package com.cts.loanservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmiPaymentRequest {

    @NotBlank
    private String accountId;

    @NotNull
    private Double amount;
}
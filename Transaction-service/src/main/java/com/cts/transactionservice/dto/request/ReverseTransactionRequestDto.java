package com.cts.transactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReverseTransactionRequestDto {
    @NotBlank(message = "A reason for reversing the transaction is required")
    @Size(min = 5, max = 500, message = "Reversal remarks must be between 5 and 500 characters")
    private String remarks;
}


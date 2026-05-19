package com.cts.transactionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelTransactionRequestDto {
    @NotBlank(message = "A reason for cancelling the transaction is required")
    @Size(min = 5, max = 500, message = "Cancellation remarks must be between 5 and 500 characters")
    private String remarks;
}


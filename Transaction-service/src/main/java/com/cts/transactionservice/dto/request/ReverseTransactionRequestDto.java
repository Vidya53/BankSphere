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
    @NotBlank(message = "Reversal reason / remarks are required")
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}


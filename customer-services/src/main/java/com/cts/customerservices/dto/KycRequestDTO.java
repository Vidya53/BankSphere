package com.cts.customerservices.dto;


import com.cts.customerservices.enums.DocumentType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRequestDTO {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(min = 5, max = 50, message = "Document number must be between 5 and 50 characters")
    private String documentNumber;

    @Future(message = "Document expiry date must be a future date")
    private LocalDateTime expiryDate;

}

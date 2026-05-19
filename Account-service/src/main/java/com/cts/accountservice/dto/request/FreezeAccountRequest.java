package com.cts.accountservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreezeAccountRequest {

    @NotBlank(message = "A reason for freezing the account is required")
    @Size(min = 5, max = 500, message = "Freeze reason must be between 5 and 500 characters")
    private String reason;
}


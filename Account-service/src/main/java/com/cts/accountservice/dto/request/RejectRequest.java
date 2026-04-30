package com.cts.accountservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    private String reason;

    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    private String remarks;
}


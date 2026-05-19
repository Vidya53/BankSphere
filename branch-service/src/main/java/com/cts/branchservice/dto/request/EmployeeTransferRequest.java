package com.cts.branchservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTransferRequest {

    @NotBlank(message = "Target branch code is required")
    @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "Branch code must be 2-20 uppercase letters or digits")
    private String targetBranchCode;

    @Size(max = 500, message = "Transfer reason must not exceed 500 characters")
    private String transferReason;
}

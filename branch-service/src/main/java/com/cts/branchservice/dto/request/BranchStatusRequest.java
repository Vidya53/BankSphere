package com.cts.branchservice.dto.request;

import com.cts.branchservice.enums.BranchStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchStatusRequest {

    @NotNull(message = "Branch status is required")
    private BranchStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

package com.cts.branchservice.dto.response;

import com.cts.branchservice.enums.BranchStatus;
import lombok.*;

/**
 * Lightweight response for internal service-to-service branch validation.
 * Contains only what calling services need to perform their own validation logic.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchValidationResponse {

    private String branchCode;
    private String branchName;
    private String ifscCode;
    private BranchStatus status;
    private boolean isActive;
    private String city;
    private String state;
    private String country;

    // Human-readable message explaining why the branch is not active (null when active)
    private String inactiveReason;
}

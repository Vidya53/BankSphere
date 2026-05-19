package com.cts.branchservice.dto.request;

import com.cts.branchservice.enums.BranchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchUpdateRequest {

    @Size(min = 3, max = 100, message = "Branch name must be between 3 and 100 characters")
    private String branchName;

    private BranchType branchType;

    @Valid
    private BranchCreateRequest.AddressRequest address;

    @Valid
    private BranchCreateRequest.ContactRequest contact;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    private Boolean hasAtm;
    private Boolean has24x7Service;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}

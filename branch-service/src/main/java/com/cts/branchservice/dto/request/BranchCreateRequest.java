package com.cts.branchservice.dto.request;

import com.cts.branchservice.enums.BranchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchCreateRequest {

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "Branch code must be 2-20 uppercase letters or digits")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(min = 3, max = 100, message = "Branch name must be between 3 and 100 characters")
    private String branchName;

    @NotNull(message = "Branch type is required")
    private BranchType branchType;

    @NotNull(message = "Address is required")
    @Valid
    private AddressRequest address;

    @NotNull(message = "Contact details are required")
    @Valid
    private ContactRequest contact;

    // Auto-generated from branchCode if not supplied (BNKS0 + 6-char branch code)
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must be 4 uppercase letters, the digit 0, then 6 alphanumerics")
    private String ifscCode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    @NotNull(message = "Please specify whether this branch has an ATM")
    private Boolean hasAtm = false;

    @NotNull(message = "Please specify whether this branch offers 24x7 service")
    private Boolean has24x7Service = false;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @Valid
    private List<OperatingHoursRequest> operatingHours;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressRequest {

        @NotBlank(message = "Address line 1 is required")
        @Size(min = 3, max = 150, message = "Address line 1 must be between 3 and 150 characters")
        private String addressLine1;

        @Size(max = 150, message = "Address line 2 must not exceed 150 characters")
        private String addressLine2;

        @NotBlank(message = "City is required")
        @Size(min = 2, max = 60, message = "City must be between 2 and 60 characters")
        private String city;

        @NotBlank(message = "State is required")
        @Size(min = 2, max = 60, message = "State must be between 2 and 60 characters")
        private String state;

        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Postal code must be a valid 6-digit Indian PIN code")
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Size(min = 2, max = 60, message = "Country must be between 2 and 60 characters")
        private String country;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactRequest {

        @NotBlank(message = "Primary phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10–15 digits")
        private String primaryPhone;

        @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Secondary phone must be 10–15 digits")
        private String secondaryPhone;

        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        private String email;

        @Size(max = 20, message = "Fax must not exceed 20 characters")
        private String fax;
    }
}

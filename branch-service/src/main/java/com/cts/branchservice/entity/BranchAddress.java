package com.cts.branchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchAddress {

    @NotBlank(message = "Address line 1 is required")
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Column(name = "city", nullable = false)
    private String city;

    @NotBlank(message = "State is required")
    @Column(name = "state", nullable = false)
    private String state;

    @Pattern(regexp = "^[0-9]{5,6}$", message = "Postal code must be 5 or 6 digits")
    @Column(name = "postal_code")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Column(name = "country", nullable = false)
    private String country;
}

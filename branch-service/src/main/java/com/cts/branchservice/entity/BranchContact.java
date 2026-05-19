package com.cts.branchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchContact {

    @NotBlank(message = "Primary phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Primary phone must be 10–15 digits")
    @Column(name = "primary_phone", nullable = false)
    private String primaryPhone;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Secondary phone must be 10–15 digits")
    @Column(name = "secondary_phone")
    private String secondaryPhone;

    @Email(message = "Branch email must be valid")
    @Column(name = "branch_email")
    private String email;

    @Column(name = "fax")
    private String fax;
}

package com.cts.customerservices.entity;

import com.cts.customerservices.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer number is mandatory")
    @Column(unique = true)
    private String customerNo;

    @Column(unique = true)
    private String userId;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    @NotNull(message = "Date of birth is mandatory")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    @NotBlank(message = "Mobile number is required")
    @Column(unique = true)
    private String mobileNumber;

    @Pattern(regexp = "^\\d{10}$", message = "Alternate mobile number must be 10 digits")
    private String alternateMobileNumber;

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotBlank(message = "Address Line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @PositiveOrZero(message = "Income amount cannot be negative")
    private Double incomeAmount;

    @Pattern(regexp = "^[0-9]{5,6}$", message = "Postal code must be 5 or 6 digits")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    private RiskCategory riskCategory;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
    private String remarks;

    private Boolean isDeleted = false; // Default value
}
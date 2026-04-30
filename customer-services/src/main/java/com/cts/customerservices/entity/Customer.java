package com.cts.customerservices.entity;


import com.cts.customerservices.enums.*;
import jakarta.persistence.*;
import lombok.*;
import sun.jvm.hotspot.debugger.cdbg.EnumType;

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

    @Column(unique = true)
    private String customerNo;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String mobileNumber;

    private String alternateMobileNumber;

    private String branchCode;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    private RiskCategory riskCategory;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private String remarks;

    private Boolean isDeleted;

}


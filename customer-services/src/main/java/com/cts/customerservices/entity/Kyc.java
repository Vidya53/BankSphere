package com.cts.customerservices.entity;

import com.cts.customerservices.enums.DocumentType;
import com.cts.customerservices.enums.KycStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kyc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer number is mandatory")
    private String customerNo;

    @NotNull(message = "Document type must be specified")
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @NotBlank(message = "Document number cannot be empty")
    @Size(min = 5, max = 50, message = "Document number should be between 5 and 50 characters")
    private String documentNumber;

    @NotNull(message = "KYC status is required")
    @Enumerated(EnumType.STRING)
    private KycStatus status;

    @PastOrPresent(message = "Submitted date cannot be in the future")
    private LocalDateTime submittedDate;

    private LocalDateTime verifiedDate;

    private String verifiedBy;

    @Size(max = 255, message = "Rejection reason is too long")
    private String rejectionReason;

    @Future(message = "Document expiry date must be a future date")
    private LocalDateTime expiryDate;

}
package com.cts.customerservices.dto;

import com.cts.customerservices.enums.DocumentType;
import com.cts.customerservices.enums.KycStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycResponseDTO {

    private String customerNo;

    private DocumentType documentType;

    private String documentNumber;

    private KycStatus status;

    private LocalDateTime submittedDate;

    private LocalDateTime verifiedDate;

    private String verifiedBy;

    private String rejectionReason;

    private LocalDateTime expiryDate;

}

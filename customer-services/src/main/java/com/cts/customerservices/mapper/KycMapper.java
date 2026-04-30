package com.cts.customerservices.mapper;


import com.cts.customerservices.dto.KycRequestDTO;
import com.cts.customerservices.dto.KycResponseDTO;
import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.KycStatus;

import java.time.LocalDateTime;

public class KycMapper {

    public static Kyc toEntity(
            KycRequestDTO dto,
            String customerNo
    ) {

        return Kyc.builder()
                .customerNo(customerNo)
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .status(KycStatus.SUBMITTED)
                .submittedDate(LocalDateTime.now())
                .build();

    }

    public static KycResponseDTO toDTO(Kyc kyc) {

        return KycResponseDTO.builder()
                .customerNo(kyc.getCustomerNo())
                .documentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .status(kyc.getStatus())
                .submittedDate(kyc.getSubmittedDate())
                .verifiedDate(kyc.getVerifiedDate())
                .verifiedBy(kyc.getVerifiedBy())
                .rejectionReason(kyc.getRejectionReason())
                .expiryDate(kyc.getExpiryDate())
                .build();

    }

}

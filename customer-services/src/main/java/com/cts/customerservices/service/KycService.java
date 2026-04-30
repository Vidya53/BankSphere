package com.cts.customerservices.service;

import com.cts.customerservices.dto.KycRequestDTO;
import com.cts.customerservices.dto.KycResponseDTO;

import java.util.List;

public interface KycService {

    KycResponseDTO submitKyc(
            String customerNo,
            KycRequestDTO request
    );

    void approveKyc(
            String customerNo
    );

    void rejectKyc(
            String customerNo,
            String reason
    );

    KycResponseDTO getKycStatus(
            String customerNo
    );

    List<KycResponseDTO> getPendingKyc();

    List<KycResponseDTO> getApprovedKyc();

    List<KycResponseDTO> getRejectedKyc();

}
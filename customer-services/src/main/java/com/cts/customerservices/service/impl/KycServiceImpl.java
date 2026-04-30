package com.cts.customerservices.service.impl;

import com.cts.customerservices.dto.KycRequestDTO;
import com.cts.customerservices.dto.KycResponseDTO;
import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.DocumentType;
import com.cts.customerservices.enums.KycStatus;
import com.cts.customerservices.exception.*;
import com.cts.customerservices.mapper.KycMapper;
import com.cts.customerservices.repository.CustomerRepository;
import com.cts.customerservices.repository.KycRepository;
import com.cts.customerservices.service.KycService;
import com.cts.customerservices.util.BusinessConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final KycRepository repository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public KycResponseDTO submitKyc(String customerNo, KycRequestDTO request) {
        log.info("Submitting KYC for customer: {}", customerNo);

        // Verify customer exists and is not deleted
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with customerNo: " + customerNo));

        if (Boolean.TRUE.equals(customer.getIsDeleted())) {
            throw new CustomerDeletedException(customerNo);
        }

        // Check for duplicate KYC submission
        if (repository.existsByCustomerNo(customerNo)) {
            throw new DuplicateKycException(customerNo);
        }

        // Validate document number format based on document type
        validateDocumentNumber(request.getDocumentType(), request.getDocumentNumber());

        Kyc kyc = KycMapper.toEntity(request, customerNo);

        // Set expiry date if provided
        if (request.getExpiryDate() != null) {
            kyc.setExpiryDate(request.getExpiryDate());
        }

        repository.save(kyc);
        log.info("KYC submitted successfully for customer: {}", customerNo);
        return KycMapper.toDTO(kyc);
    }

    @Override
    @Transactional
    public void approveKyc(String customerNo) {
        log.info("Approving KYC for customer: {}", customerNo);
        Kyc kyc = getKyc(customerNo);

        if (kyc.getStatus() == KycStatus.APPROVED) {
            throw new BusinessException("KYC is already approved for customer: " + customerNo);
        }

        if (kyc.getStatus() != KycStatus.SUBMITTED && kyc.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new BusinessException("KYC can only be approved from SUBMITTED or UNDER_REVIEW status. Current status: " + kyc.getStatus());
        }

        // Check document expiry
        if (kyc.getExpiryDate() != null && kyc.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot approve KYC with an expired document. Expiry date: " + kyc.getExpiryDate());
        }

        kyc.setStatus(KycStatus.APPROVED);
        kyc.setVerifiedDate(LocalDateTime.now());
        kyc.setVerifiedBy("SYSTEM");
        repository.save(kyc);
        log.info("KYC approved for customer: {}", customerNo);
    }

    @Override
    @Transactional
    public void rejectKyc(String customerNo, String reason) {
        log.info("Rejecting KYC for customer: {}", customerNo);

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Rejection reason is mandatory");
        }

        Kyc kyc = getKyc(customerNo);

        if (kyc.getStatus() == KycStatus.REJECTED) {
            throw new BusinessException("KYC is already rejected for customer: " + customerNo);
        }

        if (kyc.getStatus() != KycStatus.SUBMITTED && kyc.getStatus() != KycStatus.UNDER_REVIEW) {
            throw new BusinessException("KYC can only be rejected from SUBMITTED or UNDER_REVIEW status. Current status: " + kyc.getStatus());
        }

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setVerifiedDate(LocalDateTime.now());
        kyc.setVerifiedBy("SYSTEM");
        repository.save(kyc);
        log.info("KYC rejected for customer: {} with reason: {}", customerNo, reason);
    }

    private Kyc getKyc(String customerNo) {
        return repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record not found for customer: " + customerNo));
    }

    @Override
    @Transactional(readOnly = true)
    public KycResponseDTO getKycStatus(String customerNo) {
        log.debug("Fetching KYC status for customer: {}", customerNo);
        return KycMapper.toDTO(getKyc(customerNo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycResponseDTO> getPendingKyc() {
        return repository.findByStatus(KycStatus.SUBMITTED)
                .stream()
                .map(KycMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycResponseDTO> getApprovedKyc() {
        return repository.findByStatus(KycStatus.APPROVED)
                .stream()
                .map(KycMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycResponseDTO> getRejectedKyc() {
        return repository.findByStatus(KycStatus.REJECTED)
                .stream()
                .map(KycMapper::toDTO)
                .toList();
    }

    // ─── Document Validation ───

    private void validateDocumentNumber(DocumentType type, String documentNumber) {
        String pattern;
        String formatHint;

        switch (type) {
            case AADHAR:
                pattern = BusinessConstants.AADHAR_PATTERN;
                formatHint = "Aadhar number must be 12 digits starting with 2-9";
                break;
            case PAN:
                pattern = BusinessConstants.PAN_PATTERN;
                formatHint = "PAN must be in format: ABCDE1234F (5 letters, 4 digits, 1 letter)";
                break;
            case PASSPORT:
                pattern = BusinessConstants.PASSPORT_PATTERN;
                formatHint = "Passport must be in format: A1234567 (1 letter followed by 7 digits)";
                break;
            case VOTER_ID:
                pattern = BusinessConstants.VOTER_ID_PATTERN;
                formatHint = "Voter ID must be in format: ABC1234567 (3 letters followed by 7 digits)";
                break;
            default:
                throw new InvalidDocumentException("Unsupported document type: " + type);
        }

        if (!documentNumber.matches(pattern)) {
            throw new InvalidDocumentException("Invalid " + type + " number format. " + formatHint);
        }
    }
}

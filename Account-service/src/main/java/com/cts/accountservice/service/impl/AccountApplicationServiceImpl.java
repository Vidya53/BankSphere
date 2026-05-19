package com.cts.accountservice.service.impl;

import com.cts.accountservice.client.CustomerServiceClient;
import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.request.RejectRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.AccountApplication;
import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.exception.*;
import com.cts.accountservice.mapper.AccountMapper;
import com.cts.accountservice.repository.AccountApplicationRepository;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountApplicationServiceImpl implements AccountApplicationService {

    private final AccountApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final KycVerificationService kycVerificationService;
    private final BranchValidationService branchValidationService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public AccountApplicationResponse applyForAccount(AccountApplicationRequest request, UserContext userContext) {
        log.info("Customer {} applying for {} account", userContext.getUserId(), request.getAccountType());

        // Validate KYC
        if (!kycVerificationService.isKycApproved(userContext.getUserId())) {
            throw new KycNotApprovedException("KYC must be approved before applying for an account. Please complete KYC first.");
        }

        // Validate branch
        String branchCode = request.getBranchCode();
        if (!branchValidationService.isBranchActive(branchCode)) {
            throw new InvalidOperationException("Branch " + branchCode + " is not active");
        }

        // Check duplicate pending/submitted application for same account type
        boolean hasPending = applicationRepository.existsByCustomerIdAndAccountTypeAndStatusIn(
                userContext.getUserId(), request.getAccountType(),
                List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW));
        if (hasPending) {
            throw new DuplicateApplicationException(
                    "You already have a pending " + request.getAccountType() + " account application");
        }

        // Validate initial deposit for SAVINGS minimum
        if (request.getAccountType() == AccountType.SAVINGS &&
                request.getInitialDeposit() != null &&
                request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0 &&
                request.getInitialDeposit().compareTo(new BigDecimal("500.00")) < 0) {
            throw new InvalidOperationException("Minimum initial deposit for SAVINGS account is ₹500.00");
        }

        if (request.getAccountType() == AccountType.CURRENT &&
                request.getInitialDeposit() != null &&
                request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0 &&
                request.getInitialDeposit().compareTo(new BigDecimal("5000.00")) < 0) {
            throw new InvalidOperationException("Minimum initial deposit for CURRENT account is ₹5000.00");
        }

        String appRef = "APP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AccountApplication application = AccountApplication.builder()
                .applicationRef(appRef)
                .customerId(userContext.getUserId())
                .customerName(userContext.getCustomerName() != null ? userContext.getCustomerName() : userContext.getUsername())
                .customerEmail(userContext.getEmail() != null ? userContext.getEmail() : "")
                .customerPhone(userContext.getPhone())
                .branchCode(branchCode)
                .accountType(request.getAccountType())
                .initialDeposit(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO)
                .nomineeName(request.getNomineeName())
                .nomineeRelation(request.getNomineeRelation())
                .nomineePhone(request.getNomineePhone())
                .nomineeAddress(request.getNomineeAddress())
                .purpose(request.getPurpose())
                .status(ApplicationStatus.SUBMITTED)
                .build();

        application = applicationRepository.save(application);

        // Audit & Notification
        auditService.logAudit(userContext.getUserId(), userContext.getRole(), "ACCOUNT_APPLICATION_SUBMITTED",
                "ACCOUNT_APPLICATION", appRef, branchCode);
        notificationService.sendNotification(userContext.getUserId(), userContext.getEmail(),
                "Account Application Submitted",
                "Your " + request.getAccountType() + " account application (" + appRef + ") has been submitted successfully.");

        log.info("Account application {} created for customer {}", appRef, userContext.getUserId());
        return AccountMapper.toApplicationResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountApplicationResponse> getMyApplications(UserContext userContext) {
        log.debug("Fetching applications for customer {}", userContext.getUserId());
        return applicationRepository.findByCustomerIdOrderByCreatedAtDesc(userContext.getUserId())
                .stream().map(AccountMapper::toApplicationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountApplicationResponse getApplicationById(Long id, UserContext userContext) {
        AccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Customers can only view their own
        if ("CUSTOMER".equals(userContext.getRole()) && !app.getCustomerId().equals(userContext.getUserId())) {
            throw new UnauthorizedBranchAccessException("You can only view your own applications");
        }

        return AccountMapper.toApplicationResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountApplicationResponse> getPendingApplicationsByBranch(String branchCode) {
        log.debug("Fetching pending applications for branch {}", branchCode);
        return applicationRepository.findByBranchCodeAndStatus(branchCode, ApplicationStatus.SUBMITTED)
                .stream().map(AccountMapper::toApplicationResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountApplicationResponse> getAllApplicationsByBranch(String branchCode) {
        return applicationRepository.findByBranchCode(branchCode)
                .stream().map(AccountMapper::toApplicationResponse).toList();
    }

    @Override
    public AccountApplicationResponse approveApplication(Long id, UserContext staffContext) {
        log.info("Staff {} approving application {}", staffContext.getUserId(), id);

        AccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validate branch scope
        if (!app.getBranchCode().equals(staffContext.getBranchCode())) {
            throw new UnauthorizedBranchAccessException("You can only approve applications within your branch");
        }

        if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new InvalidOperationException("Application is not in a reviewable state. Current status: " + app.getStatus());
        }

        // Re-validate KYC at approval time
        if (!kycVerificationService.isKycApproved(app.getCustomerId())) {
            throw new KycNotApprovedException("Customer KYC is no longer approved. Cannot approve account application.");
        }

        // Generate unique account number
        String accountNo = generateAccountNo(app.getAccountType());

        // Determine minimum balance based on account type
        BigDecimal minBalance = app.getAccountType() == AccountType.SAVINGS ?
                new BigDecimal("500.00") : new BigDecimal("5000.00");

        BigDecimal initialBalance = app.getInitialDeposit() != null ? app.getInitialDeposit() : BigDecimal.ZERO;

        String ifscCode = branchValidationService.getIfscCode(app.getBranchCode());

        // Create account
        Account account = Account.builder()
                .accountNo(accountNo)
                .customerId(app.getCustomerId())
                .customerName(app.getCustomerName())
                .customerEmail(app.getCustomerEmail())
                .customerPhone(app.getCustomerPhone())
                .branchCode(app.getBranchCode())
                .ifscCode(ifscCode)
                .accountType(app.getAccountType())
                .balance(initialBalance)
                .minimumBalance(minBalance)
                .status(AccountStatus.ACTIVE)
                .nomineeName(app.getNomineeName())
                .nomineeRelation(app.getNomineeRelation())
                .nomineePhone(app.getNomineePhone())
                .nomineeAddress(app.getNomineeAddress())
                .approvedBy(staffContext.getUserId())
                .isTransactional(true)
                .dailyTransferLimit(new BigDecimal("500000.00"))
                .dailyWithdrawalLimit(new BigDecimal("200000.00"))
                .build();

        accountRepository.save(account);

        // Update application
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedBy(staffContext.getUserId());
        app.setReviewedAt(LocalDateTime.now());
        app.setGeneratedAccountNo(accountNo);
        applicationRepository.save(app);

        // Auto-activate the customer. Account approval already required KYC to be
        // APPROVED, so the customer has satisfied register + KYC + account.
        // Best-effort: failure here must not roll back account creation — the
        // customer can still be promoted manually from the staff console.
        try {
            Boolean activated = customerServiceClient.activateCustomerByUserId(app.getCustomerId());
            if (Boolean.TRUE.equals(activated)) {
                log.info("Customer {} auto-activated after application {} approval",
                        app.getCustomerId(), app.getApplicationRef());
            }
        } catch (Exception ex) {
            log.warn("Customer auto-activation failed for {} — manual activation may be required: {}",
                    app.getCustomerId(), ex.getMessage());
        }

        // Audit & Notification
        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_APPLICATION_APPROVED",
                "ACCOUNT_APPLICATION", app.getApplicationRef(), app.getBranchCode());
        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_CREATED",
                "ACCOUNT", accountNo, app.getBranchCode());
        notificationService.sendNotification(app.getCustomerId(), app.getCustomerEmail(),
                "Account Application Approved",
                "Your " + app.getAccountType() + " account has been approved. Account No: " + accountNo);

        log.info("Application {} approved. Account {} created", app.getApplicationRef(), accountNo);
        return AccountMapper.toApplicationResponse(app);
    }

    @Override
    public AccountApplicationResponse rejectApplication(Long id, RejectRequest request, UserContext staffContext) {
        log.info("Staff {} rejecting application {}", staffContext.getUserId(), id);

        AccountApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (!app.getBranchCode().equals(staffContext.getBranchCode())) {
            throw new UnauthorizedBranchAccessException("You can only reject applications within your branch");
        }

        if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new InvalidOperationException("Application is not in a reviewable state. Current status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(request.getReason());
        app.setRemarks(request.getRemarks());
        app.setReviewedBy(staffContext.getUserId());
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_APPLICATION_REJECTED",
                "ACCOUNT_APPLICATION", app.getApplicationRef(), app.getBranchCode());
        notificationService.sendNotification(app.getCustomerId(), app.getCustomerEmail(),
                "Account Application Rejected",
                "Your " + app.getAccountType() + " account application has been rejected. Reason: " + request.getReason());

        log.info("Application {} rejected", app.getApplicationRef());
        return AccountMapper.toApplicationResponse(app);
    }

    private String generateAccountNo(AccountType type) {
        String prefix = switch (type) {
            case SAVINGS           -> "SAV";
            case CURRENT           -> "CUR";
            case FIXED_DEPOSIT     -> "FDR";
            case RECURRING_DEPOSIT -> "RDP";
        };
        String accountNo;
        int attempts = 0;
        do {
            // UUID-based: take first 14 hex chars, convert to uppercase decimal-like string
            String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
            accountNo = prefix + uuidPart;
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Failed to generate a unique account number after " + attempts + " attempts.");
            }
        } while (accountRepository.existsByAccountNo(accountNo));
        return accountNo;
    }
}


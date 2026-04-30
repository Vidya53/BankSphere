package com.cts.accountservice.config;

import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.AccountApplication;
import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.repository.AccountApplicationRepository;
import com.cts.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds mock data for testing without inter-service dependencies.
 * This data simulates what would normally come from identity-service, branch-service, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final AccountApplicationRepository applicationRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            return;
        }

        log.info("=== Initializing mock data for Account Service ===");

        // --- Existing accounts (pre-approved) ---
        Account acc1 = Account.builder()
                .accountNo("SAV1000000001")
                .customerId("CUST001")
                .customerName("Rajesh Kumar")
                .customerEmail("rajesh.kumar@email.com")
                .customerPhone("9876543210")
                .branchCode("BR001")
                .ifscCode("BNKS0BR001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("25000.00"))
                .minimumBalance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .nomineeName("Priya Kumar")
                .nomineeRelation("Spouse")
                .nomineePhone("9876543211")
                .approvedBy("STAFF001")
                .isTransactional(true)
                .dailyTransferLimit(new BigDecimal("500000.00"))
                .dailyWithdrawalLimit(new BigDecimal("200000.00"))
                .build();

        Account acc2 = Account.builder()
                .accountNo("CUR1000000001")
                .customerId("CUST002")
                .customerName("Anita Sharma")
                .customerEmail("anita.sharma@email.com")
                .customerPhone("9876543220")
                .branchCode("BR001")
                .ifscCode("BNKS0BR001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("150000.00"))
                .minimumBalance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .nomineeName("Vikram Sharma")
                .nomineeRelation("Husband")
                .approvedBy("STAFF001")
                .isTransactional(true)
                .dailyTransferLimit(new BigDecimal("500000.00"))
                .dailyWithdrawalLimit(new BigDecimal("200000.00"))
                .build();

        Account acc3 = Account.builder()
                .accountNo("SAV1000000002")
                .customerId("CUST003")
                .customerName("Mohammed Ali")
                .customerEmail("mohammed.ali@email.com")
                .customerPhone("9876543230")
                .branchCode("BR002")
                .ifscCode("BNKS0BR002")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .minimumBalance(new BigDecimal("500.00"))
                .status(AccountStatus.FROZEN)
                .freezeReason("Suspicious activity reported")
                .frozenBy("STAFF002")
                .nomineeName("Fatima Ali")
                .nomineeRelation("Spouse")
                .approvedBy("STAFF002")
                .isTransactional(false)
                .dailyTransferLimit(new BigDecimal("500000.00"))
                .dailyWithdrawalLimit(new BigDecimal("200000.00"))
                .build();

        Account acc4 = Account.builder()
                .accountNo("SAV1000000003")
                .customerId("CUST001")
                .customerName("Rajesh Kumar")
                .customerEmail("rajesh.kumar@email.com")
                .customerPhone("9876543210")
                .branchCode("BR001")
                .ifscCode("BNKS0BR001")
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("75000.00"))
                .minimumBalance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .approvedBy("STAFF001")
                .isTransactional(true)
                .dailyTransferLimit(new BigDecimal("500000.00"))
                .dailyWithdrawalLimit(new BigDecimal("200000.00"))
                .build();

        accountRepository.save(acc1);
        accountRepository.save(acc2);
        accountRepository.save(acc3);
        accountRepository.save(acc4);

        // --- Pending applications ---
        AccountApplication app1 = AccountApplication.builder()
                .applicationRef("APP00000001")
                .customerId("CUST004")
                .customerName("Deepika Patel")
                .customerEmail("deepika.patel@email.com")
                .customerPhone("9876543240")
                .branchCode("BR001")
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("10000.00"))
                .nomineeName("Suresh Patel")
                .nomineeRelation("Father")
                .purpose("Salary account")
                .status(ApplicationStatus.SUBMITTED)
                .build();

        AccountApplication app2 = AccountApplication.builder()
                .applicationRef("APP00000002")
                .customerId("CUST005")
                .customerName("Sunil Verma")
                .customerEmail("sunil.verma@email.com")
                .customerPhone("9876543250")
                .branchCode("BR001")
                .accountType(AccountType.CURRENT)
                .initialDeposit(new BigDecimal("50000.00"))
                .purpose("Business account")
                .status(ApplicationStatus.SUBMITTED)
                .build();

        AccountApplication app3 = AccountApplication.builder()
                .applicationRef("APP00000003")
                .customerId("CUST006")
                .customerName("Lakshmi Nair")
                .customerEmail("lakshmi.nair@email.com")
                .customerPhone("9876543260")
                .branchCode("BR002")
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("2000.00"))
                .nomineeName("Gopal Nair")
                .nomineeRelation("Husband")
                .status(ApplicationStatus.SUBMITTED)
                .build();

        AccountApplication app4 = AccountApplication.builder()
                .applicationRef("APP00000004")
                .customerId("CUST001")
                .customerName("Rajesh Kumar")
                .customerEmail("rajesh.kumar@email.com")
                .branchCode("BR001")
                .accountType(AccountType.FIXED_DEPOSIT)
                .initialDeposit(new BigDecimal("100000.00"))
                .purpose("Long term savings")
                .status(ApplicationStatus.REJECTED)
                .rejectionReason("FD product not yet available")
                .reviewedBy("STAFF001")
                .build();

        applicationRepository.save(app1);
        applicationRepository.save(app2);
        applicationRepository.save(app3);
        applicationRepository.save(app4);

        log.info("=== Mock data initialization complete ===");
        log.info("Accounts created: 4 | Applications created: 4");
        log.info("Branch BR001: 3 accounts, 2 pending apps");
        log.info("Branch BR002: 1 account (frozen), 1 pending app");
    }
}


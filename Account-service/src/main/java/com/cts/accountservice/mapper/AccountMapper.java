package com.cts.accountservice.mapper;

import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.entity.AccountApplication;

public class AccountMapper {

    private AccountMapper() {}

    public static AccountApplicationResponse toApplicationResponse(AccountApplication app) {
        return AccountApplicationResponse.builder()
                .id(app.getId())
                .applicationRef(app.getApplicationRef())
                .customerId(app.getCustomerId())
                .customerName(app.getCustomerName())
                .branchCode(app.getBranchCode())
                .accountType(app.getAccountType())
                .initialDeposit(app.getInitialDeposit())
                .nomineeName(app.getNomineeName())
                .nomineeRelation(app.getNomineeRelation())
                .purpose(app.getPurpose())
                .status(app.getStatus())
                .rejectionReason(app.getRejectionReason())
                .reviewedBy(app.getReviewedBy())
                .reviewedAt(app.getReviewedAt())
                .generatedAccountNo(app.getGeneratedAccountNo())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    public static AccountResponse toAccountResponse(Account acc) {
        return AccountResponse.builder()
                .id(acc.getId())
                .accountNo(acc.getAccountNo())
                .customerId(acc.getCustomerId())
                .customerName(acc.getCustomerName())
                .branchCode(acc.getBranchCode())
                .ifscCode(acc.getIfscCode())
                .accountType(acc.getAccountType())
                .balance(acc.getBalance())
                .minimumBalance(acc.getMinimumBalance())
                .status(acc.getStatus())
                .nomineeName(acc.getNomineeName())
                .nomineeRelation(acc.getNomineeRelation())
                .isTransactional(acc.getIsTransactional())
                .dailyTransferLimit(acc.getDailyTransferLimit())
                .dailyWithdrawalLimit(acc.getDailyWithdrawalLimit())
                .openedAt(acc.getOpenedAt())
                .updatedAt(acc.getUpdatedAt())
                .freezeReason(acc.getFreezeReason())
                .frozenAt(acc.getFrozenAt())
                .closeReason(acc.getCloseReason())
                .closedAt(acc.getClosedAt())
                .build();
    }
}


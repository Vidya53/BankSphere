package com.cts.accountservice.service;

import com.cts.accountservice.dto.request.CloseAccountRequest;
import com.cts.accountservice.dto.request.FreezeAccountRequest;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.BranchAccountSummary;
import com.cts.accountservice.security.UserContext;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    List<AccountResponse> getMyAccounts(UserContext userContext);

    AccountResponse getAccountByAccountNo(String accountNo);

    List<AccountResponse> getBranchAccounts(String branchCode, String status);

    AccountResponse freezeAccount(String accountNo, FreezeAccountRequest request, UserContext staffContext);

    AccountResponse unfreezeAccount(String accountNo, UserContext staffContext);

    AccountResponse closeAccount(String accountNo, CloseAccountRequest request, UserContext staffContext);

    BranchAccountSummary getBranchSummary(String branchCode);

    // Internal endpoints for transaction-service
    boolean isAccountActive(String accountNo);

    BigDecimal getBalance(String accountNo);

    boolean creditAccount(String accountNo, BigDecimal amount);

    boolean debitAccount(String accountNo, BigDecimal amount);
}


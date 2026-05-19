package com.cts.accountservice.service;

import com.cts.accountservice.dto.request.CloseAccountRequest;
import com.cts.accountservice.dto.request.FreezeAccountRequest;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.BranchAccountSummary;
import com.cts.accountservice.dto.response.BranchAccountTypeBreakdown;
import com.cts.accountservice.context.UserContext;

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

    List<BranchAccountTypeBreakdown> getBranchAccountTypeBreakdown(String branchCode);

    /**
     * Cascade-close every account belonging to the given customer (identity
     * userId). Called by customer-service when an admin or branch manager
     * soft-deletes the customer. Returns the number of accounts closed.
     */
    int closeAllAccountsForCustomer(String customerId, String reason, String closedBy);

    // Internal endpoints for transaction-service
    boolean isAccountActive(String accountNo);

    BigDecimal getBalance(String accountNo);

    boolean creditAccount(String accountNo, BigDecimal amount);

    boolean debitAccount(String accountNo, BigDecimal amount);
}


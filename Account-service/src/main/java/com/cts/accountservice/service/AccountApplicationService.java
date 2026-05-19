package com.cts.accountservice.service;

import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.request.RejectRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.context.UserContext;

import java.util.List;

public interface AccountApplicationService {

    AccountApplicationResponse applyForAccount(AccountApplicationRequest request, UserContext userContext);

    List<AccountApplicationResponse> getMyApplications(UserContext userContext);

    AccountApplicationResponse getApplicationById(Long id, UserContext userContext);

    List<AccountApplicationResponse> getPendingApplicationsByBranch(String branchCode);

    List<AccountApplicationResponse> getAllApplicationsByBranch(String branchCode);

    AccountApplicationResponse approveApplication(Long id, UserContext staffContext);

    AccountApplicationResponse rejectApplication(Long id, RejectRequest request, UserContext staffContext);
}


package com.cts.accountservice.service;

public interface BranchValidationService {

    boolean isBranchActive(String branchCode);

    String getIfscCode(String branchCode);
}

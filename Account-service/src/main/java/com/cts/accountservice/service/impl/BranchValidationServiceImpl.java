package com.cts.accountservice.service.impl;

import com.cts.accountservice.client.BranchServiceClient;
import com.cts.accountservice.service.BranchValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchValidationServiceImpl implements BranchValidationService {

    private final BranchServiceClient branchServiceClient;

    @Override
    public boolean isBranchActive(String branchCode) {
        boolean active = branchServiceClient.isBranchActive(branchCode);
        log.debug("Branch active check: branchCode={} active={}", branchCode, active);
        return active;
    }

    @Override
    public String getIfscCode(String branchCode) {
        String ifsc = branchServiceClient.getIfscCode(branchCode);
        log.debug("IFSC lookup: branchCode={} ifsc={}", branchCode, ifsc);
        return ifsc;
    }
}

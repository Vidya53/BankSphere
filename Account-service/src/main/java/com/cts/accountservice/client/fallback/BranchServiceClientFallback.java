package com.cts.accountservice.client.fallback;

import com.cts.accountservice.client.BranchServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BranchServiceClientFallback implements BranchServiceClient {

    @Override
    public boolean isBranchActive(String branchCode) {
        log.warn("branch-service unavailable — isBranchActive({}) fallback returning false (fail-safe)", branchCode);
        return false;
    }

    @Override
    public String getIfscCode(String branchCode) {
        log.warn("branch-service unavailable — getIfscCode({}) fallback using generated format", branchCode);
        String normalized = branchCode.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (normalized.length() > 6) normalized = normalized.substring(0, 6);
        while (normalized.length() < 6) normalized += "0";
        return "BNKS0" + normalized;
    }
}

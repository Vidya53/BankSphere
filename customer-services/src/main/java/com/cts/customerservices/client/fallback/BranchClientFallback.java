package com.cts.customerservices.client.fallback;

import com.cts.customerservices.client.BranchClient;
import com.cts.customerservices.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BranchClientFallback implements BranchClient {

    @Override
    public boolean isBranchActive(String branchCode) {
        log.error("branch-service unavailable — cannot validate branch: {}", branchCode);
        throw new ServiceUnavailableException("Branch Service");
    }
}

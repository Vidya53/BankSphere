package com.cts.branchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class IdentityStaffClientFallback implements IdentityStaffClient {
    @Override
    public Map<String, Object> staffByBranch(String branchCode) {
        log.warn("identity-service unavailable — branch staff fallback for {}", branchCode);
        return Map.of("data", List.of());
    }
}

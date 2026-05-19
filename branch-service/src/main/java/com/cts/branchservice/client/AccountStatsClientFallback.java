package com.cts.branchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AccountStatsClientFallback implements AccountStatsClient {
    @Override
    public Map<String, Object> branchStats(String branchCode) {
        log.warn("account-service unavailable — branch stats fallback for {}", branchCode);
        return Map.of();
    }
}

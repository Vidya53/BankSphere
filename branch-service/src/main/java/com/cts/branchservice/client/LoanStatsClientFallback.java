package com.cts.branchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class LoanStatsClientFallback implements LoanStatsClient {
    @Override
    public Map<String, Object> mtd() {
        log.warn("loan-service unavailable — loan MTD fallback");
        return Map.of();
    }
}

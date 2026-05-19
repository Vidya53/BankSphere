package com.cts.branchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "loan-service", fallback = LoanStatsClientFallback.class)
public interface LoanStatsClient {

    @GetMapping("/api/v1/internal/stats/loans/mtd")
    Map<String, Object> mtd();
}

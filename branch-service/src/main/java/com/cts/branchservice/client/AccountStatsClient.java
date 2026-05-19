package com.cts.branchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Fetches per-branch account aggregates from account-service. Used by the
 * branch-manager dashboard. The endpoint is internal-only (not gateway-routed)
 * and reachable via Eureka service discovery.
 */
@FeignClient(name = "account-service", fallback = AccountStatsClientFallback.class)
public interface AccountStatsClient {

    @GetMapping("/api/v1/internal/stats/accounts/by-branch/{branchCode}")
    Map<String, Object> branchStats(@PathVariable("branchCode") String branchCode);
}

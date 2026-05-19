package com.cts.branchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Fetches staff for a branch from identity-service. Identity is the source of
 * truth for staff users (the local Employee table is used only for legacy
 * branch ops). The endpoint returns a thin DTO list.
 */
@FeignClient(name = "identity-service", fallback = IdentityStaffClientFallback.class)
public interface IdentityStaffClient {

    @GetMapping("/api/v1/admin/staff/by-branch/{branchCode}")
    Map<String, Object> staffByBranch(@PathVariable("branchCode") String branchCode);
}

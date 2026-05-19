package com.cts.customerservices.client;

import com.cts.customerservices.client.fallback.BranchClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "branch-service",
        fallback = BranchClientFallback.class
)
public interface BranchClient {

    @GetMapping("/api/v1/internal/branches/{branchCode}/active")
    boolean isBranchActive(@PathVariable("branchCode") String branchCode);
}

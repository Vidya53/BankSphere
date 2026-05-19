package com.cts.accountservice.client;

import com.cts.accountservice.client.fallback.BranchServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "branch-service", fallback = BranchServiceClientFallback.class)
public interface BranchServiceClient {

    @GetMapping("/api/v1/internal/branches/{branchCode}/active")
    boolean isBranchActive(@PathVariable("branchCode") String branchCode);

    @GetMapping("/api/v1/internal/branches/{branchCode}/ifsc")
    String getIfscCode(@PathVariable("branchCode") String branchCode);
}

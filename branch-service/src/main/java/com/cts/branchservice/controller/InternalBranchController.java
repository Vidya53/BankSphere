package com.cts.branchservice.controller;

import com.cts.branchservice.dto.response.BranchValidationResponse;
import com.cts.branchservice.service.BranchService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal service-to-service API. Not exposed via API Gateway.
 * Called by customer-service, account-service via Feign.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/internal/branches")
@RequiredArgsConstructor
@Tag(name = "Internal · Branch Lookup", description = "Internal branch lookups used by customer-service and account-service via Feign")
public class InternalBranchController {

    private final BranchService branchService;

    @Operation(
            summary = "Check whether a branch is active",
            description = """
                    Returns `true` if the branch exists, is non-deleted, and currently has status=ACTIVE. Used by other services to gate operations that require an active branch.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/{branchCode}/active")
    public boolean isBranchActive(@PathVariable String branchCode) {
        return branchService.isBranchActive(branchCode);
    }

    @Operation(
            summary = "Validate a branch for downstream service calls",
            description = """
                    Returns a compact validation payload (branch code, name, IFSC, status) used by customer-service and account-service when binding a new entity to a branch.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/{branchCode}/validate")
    public BranchValidationResponse validateBranch(@PathVariable String branchCode) {
        return branchService.getBranchForValidation(branchCode);
    }

    @Operation(
            summary = "Resolve IFSC code for a branch",
            description = """
                    Returns the IFSC code for the given branch — either the stored value or the auto-generated `BNKS0<padded-branch-code>` if none was supplied at creation.

                    **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    @GetMapping("/{branchCode}/ifsc")
    public String getIfscCode(@PathVariable String branchCode) {
        return branchService.getIfscCode(branchCode);
    }
}

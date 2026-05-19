package com.cts.branchservice.controller;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.BranchStatusRequest;
import com.cts.branchservice.dto.request.BranchUpdateRequest;
import com.cts.branchservice.dto.request.OperatingHoursRequest;
import com.cts.branchservice.dto.response.BranchResponse;
import com.cts.branchservice.dto.response.BranchSummaryResponse;
import com.cts.branchservice.dto.response.OperatingHoursResponse;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import com.cts.branchservice.service.BranchService;
import com.cts.branchservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Admin APIs for creating and managing bank branches")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Create a new branch",
            description = """
                    Creates a new bank branch. If no IFSC code is supplied, one is auto-generated as `BNKS0<padded-branch-code>`. Address, operating hours, and branch type are persisted in a single transaction.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN
                    **Side effects:** Persists a new Branch row with status=ACTIVE."""
    )
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @Valid @RequestBody BranchCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String createdBy) {

        BranchResponse response = branchService.createBranch(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Branch created successfully"));
    }

    @GetMapping("/{branchCode}")
    @Operation(
            summary = "Get branch by code",
            description = """
                    Fetches a single branch by its unique branch code. Returns the full branch record (address, IFSC, type, status, manager, operating hours).

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<BranchResponse>> getBranch(
            @PathVariable String branchCode) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.getBranchByCode(branchCode), "Branch retrieved successfully"));
    }

    @PutMapping("/{branchCode}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Update branch details",
            description = """
                    Updates editable fields on an existing branch (name, address, contact, type). Branch code and IFSC are immutable.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable String branchCode,
            @Valid @RequestBody BranchUpdateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String updatedBy) {

        BranchResponse response = branchService.updateBranch(branchCode, request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(response, "Branch updated successfully"));
    }

    @PatchMapping("/{branchCode}/status")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Update branch status",
            description = """
                    Transitions a branch between ACTIVE, INACTIVE, UNDER_MAINTENANCE, and CLOSED. Used to take a branch offline for maintenance or permanent closure.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Void>> updateBranchStatus(
            @PathVariable String branchCode,
            @Valid @RequestBody BranchStatusRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String updatedBy) {

        branchService.updateBranchStatus(branchCode, request, updatedBy);
        return ResponseEntity.ok(ApiResponse.success("Branch status updated to " + request.getStatus()));
    }

    @DeleteMapping("/{branchCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Soft-delete a branch",
            description = """
                    Marks a branch as deleted by setting `isDeleted=true` and status=INACTIVE. The record is preserved for audit and historical reporting; it is excluded from default branch listings.

                    **Allowed roles:** ADMIN
                    **Side effects:** Sets isDeleted=true, status=INACTIVE; branch hidden from active queries."""
    )
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable String branchCode,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String deletedBy) {

        branchService.deleteBranch(branchCode, deletedBy);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted successfully"));
    }

    @GetMapping
    @Operation(
            summary = "List all branches with optional filters and pagination",
            description = """
                    Returns a paginated list of branches with optional filters on status, branch type, city, and state. Soft-deleted branches are excluded from results.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<Page<BranchResponse>>> getAllBranches(
            @Parameter(description = "Filter by status") @RequestParam(required = false) BranchStatus status,
            @Parameter(description = "Filter by branch type") @RequestParam(required = false) BranchType branchType,
            @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
            @Parameter(description = "Filter by state") @RequestParam(required = false) String state,
            @PageableDefault(size = 20, sort = "branchName") Pageable pageable) {

        Page<BranchResponse> page = branchService.getAllBranches(status, branchType, city, state, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Branches retrieved successfully"));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search branches",
            description = """
                    Full-text search across branch name, branch code, city, state, and IFSC code. Useful for the locator UI and the admin console's quick-find.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<BranchResponse>>> searchBranches(
            @RequestParam String q) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.searchBranches(q), "Search results"));
    }

    @GetMapping("/by-state/{state}")
    @Operation(
            summary = "Get all branches in a state",
            description = """
                    Returns every non-deleted branch located in the supplied state name.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByState(
            @PathVariable String state) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.getBranchesByState(state), "Branches in " + state));
    }

    @GetMapping("/by-city/{city}")
    @Operation(
            summary = "Get all branches in a city",
            description = """
                    Returns every non-deleted branch located in the supplied city name.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByCity(
            @PathVariable String city) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.getBranchesByCity(city), "Branches in " + city));
    }

    @GetMapping("/{branchCode}/summary")
    @Operation(
            summary = "Get full branch summary",
            description = """
                    Returns a composite view of the branch: profile, operating hours, current manager, headcount, and live open/closed status. One call powers the branch landing page.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<BranchSummaryResponse>> getBranchSummary(
            @PathVariable String branchCode) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.getBranchSummary(branchCode), "Branch summary retrieved"));
    }

    @GetMapping("/{branchCode}/open")
    @Operation(
            summary = "Check if branch is currently open",
            description = """
                    Returns `true` if the current server time falls within the branch's operating hours for today's day-of-week.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<Boolean>> isBranchOpen(
            @PathVariable String branchCode) {

        boolean open = branchService.isBranchCurrentlyOpen(branchCode);
        String msg = open ? "Branch is currently open" : "Branch is currently closed";
        return ResponseEntity.ok(ApiResponse.success(open, msg));
    }

    // ── Operating Hours ───────────────────────────────────────────────────────

    @PutMapping("/{branchCode}/operating-hours")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN')")
    @Operation(
            summary = "Set or update operating hours",
            description = """
                    Upserts the branch's operating-hours configuration keyed by day-of-week. Each entry specifies open time, close time, and whether the branch is closed that day.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<OperatingHoursResponse>>> setOperatingHours(
            @PathVariable String branchCode,
            @Valid @RequestBody List<OperatingHoursRequest> requests,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String updatedBy) {

        List<OperatingHoursResponse> result = branchService.setOperatingHours(branchCode, requests, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(result, "Operating hours updated"));
    }

    @GetMapping("/{branchCode}/operating-hours")
    @Operation(
            summary = "Get operating hours for a branch",
            description = """
                    Returns the branch's full operating-hours table, one row per day of week.

                    **Allowed roles:** Any authenticated user"""
    )
    public ResponseEntity<ApiResponse<List<OperatingHoursResponse>>> getOperatingHours(
            @PathVariable String branchCode) {

        return ResponseEntity.ok(ApiResponse.success(
                branchService.getOperatingHours(branchCode), "Operating hours retrieved"));
    }
}

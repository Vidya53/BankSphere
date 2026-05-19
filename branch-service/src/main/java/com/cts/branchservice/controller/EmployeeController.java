package com.cts.branchservice.controller;

import com.cts.branchservice.dto.request.EmployeeRequest;
import com.cts.branchservice.dto.request.EmployeeTransferRequest;
import com.cts.branchservice.dto.response.EmployeeResponse;
import com.cts.branchservice.enums.EmployeeStatus;
import com.cts.branchservice.service.EmployeeService;
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

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN')")
@Tag(name = "Employees", description = "APIs for managing branch employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    // ── Per-Branch Employee Endpoints ─────────────────────────────────────────

    @PostMapping("/api/v1/branches/{branchCode}/employees")
    @Operation(
            summary = "Add an employee to a branch",
            description = """
                    Creates a new employee record and assigns them to the specified branch. The branch must exist and be non-deleted; the employee's status defaults to ACTIVE.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> addEmployee(
            @PathVariable String branchCode,
            @Valid @RequestBody EmployeeRequest request) {

        EmployeeResponse response = employeeService.addEmployee(branchCode, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Employee added successfully"));
    }

    @GetMapping("/api/v1/branches/{branchCode}/employees")
    @Operation(
            summary = "List employees of a branch",
            description = """
                    Returns a paginated list of employees assigned to the branch, with an optional filter on employee status (ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED).

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> getBranchEmployees(
            @PathVariable String branchCode,
            @Parameter(description = "Filter by employee status") @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20, sort = "firstName") Pageable pageable) {

        Page<EmployeeResponse> page = employeeService.getBranchEmployees(branchCode, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Employees retrieved successfully"));
    }

    @GetMapping("/api/v1/branches/{branchCode}/manager")
    @Operation(
            summary = "Get the current branch manager",
            description = """
                    Returns the employee currently designated as manager for this branch, or 404 if no manager is assigned.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> getBranchManager(
            @PathVariable String branchCode) {

        return ResponseEntity.ok(ApiResponse.success(
                employeeService.getBranchManager(branchCode), "Branch manager retrieved"));
    }

    @PutMapping("/api/v1/branches/{branchCode}/manager/{employeeCode}")
    @Operation(
            summary = "Assign an employee as branch manager",
            description = """
                    Designates the given employee as branch manager. The employee must already be assigned to the specified branch; cross-branch assignment is rejected.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN
                    **Side effects:** Updates the branch's `branchManagerCode`."""
    )
    public ResponseEntity<ApiResponse<Void>> assignBranchManager(
            @PathVariable String branchCode,
            @PathVariable String employeeCode) {

        employeeService.assignBranchManager(branchCode, employeeCode);
        return ResponseEntity.ok(ApiResponse.success(
                "Employee " + employeeCode + " assigned as branch manager for " + branchCode));
    }

    // ── Individual Employee Endpoints ──────────────────────────────────────────

    @GetMapping("/api/v1/employees/{employeeCode}")
    @Operation(
            summary = "Get an employee by employee code",
            description = """
                    Fetches a single employee by their unique employee code, regardless of which branch they currently belong to.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(
            @PathVariable String employeeCode) {

        return ResponseEntity.ok(ApiResponse.success(
                employeeService.getEmployee(employeeCode), "Employee retrieved successfully"));
    }

    @PutMapping("/api/v1/employees/{employeeCode}")
    @Operation(
            summary = "Update employee details",
            description = """
                    Performs a partial update of employee fields — only the values supplied in the request body are modified; all others remain unchanged.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable String employeeCode,
            @Valid @RequestBody EmployeeRequest request) {

        EmployeeResponse response = employeeService.updateEmployee(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Employee updated successfully"));
    }

    @PatchMapping("/api/v1/employees/{employeeCode}/status")
    @Operation(
            summary = "Update employee status",
            description = """
                    Transitions an employee between ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, and RESIGNED states. Used for leave management and offboarding workflows.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Void>> updateEmployeeStatus(
            @PathVariable String employeeCode,
            @RequestParam EmployeeStatus status) {

        employeeService.updateEmployeeStatus(employeeCode, status);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated to " + status));
    }

    @PostMapping("/api/v1/employees/{employeeCode}/transfer")
    @Operation(
            summary = "Transfer an employee to another branch",
            description = """
                    Moves an employee from their current branch to the supplied target branch. Both branches must exist and be active; manager designation is cleared if the employee was managing the source branch.

                    **Allowed roles:** BRANCH_MANAGER, ADMIN
                    **Side effects:** Updates the employee's `branchCode`; clears stale manager link if any."""
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> transferEmployee(
            @PathVariable String employeeCode,
            @Valid @RequestBody EmployeeTransferRequest request) {

        EmployeeResponse response = employeeService.transferEmployee(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Employee transferred to branch " + request.getTargetBranchCode()));
    }
}

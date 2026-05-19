package com.cts.customerservices.controller;

import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.dto.LoanApplicationRequest;
import com.cts.customerservices.dto.LoanEligibilityResponse;
import com.cts.customerservices.payload.ApiResponse;
import com.cts.customerservices.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','LOAN_OFFICER','ADMIN')")
@Tag(name = "Customers", description = "Customer registration, profile management, lifecycle status, and loan eligibility evaluation")
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    @Operation(
        summary = "Register a new customer",
        description = """
                Creates a customer profile linked to the caller's identity user (via X-User-Id). The
                profile starts in REGISTERED status and must complete KYC before it can be activated.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                **Side effects:** Persists a new customer row; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> registerCustomer(
            @Valid @RequestBody CustomerRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.registerCustomer(request, userId), "Customer registered successfully"));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get the authenticated user's customer profile",
        description = """
                Resolves the caller's customer profile using the X-User-Id header injected by the API
                gateway. Returns 404 if no profile has been created yet.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMyProfile(userId), "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    @Operation(
        summary = "Update the authenticated user's customer profile",
        description = """
                Updates editable fields on the caller's own customer profile. Identity-sensitive fields
                (customerNo, userId, status) cannot be modified through this endpoint.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                **Side effects:** Persists the profile update; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateMyProfile(
            @Valid @RequestBody CustomerRequestDTO request,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(service.updateMyProfile(userId, request), "Profile updated successfully"));
    }

    @GetMapping("/{customerNo}")
    @Operation(
        summary = "Get a customer by customer number",
        description = """
                Returns the full customer profile for the given customerNo. Throws if the customer does
                not exist or is soft-deleted.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomer(@PathVariable String customerNo) {
        return ResponseEntity.ok(ApiResponse.success(service.getCustomer(customerNo), "Customer retrieved successfully"));
    }

    @PutMapping("/{customerNo}")
    @Operation(
        summary = "Update a customer profile by customer number",
        description = """
                Staff-grade profile update — used by CSRs and branch managers when assisting a customer.
                Caller must satisfy the class-level role check.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                **Side effects:** Persists the profile update; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
            @PathVariable String customerNo,
            @Valid @RequestBody CustomerRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateCustomer(customerNo, request), "Customer updated successfully"));
    }

    @DeleteMapping("/{customerNo}")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Operation(
        summary = "Soft-delete a customer by customer number",
        description = """
                Marks the customer row as deleted (does not physically remove it). Downstream services
                treat soft-deleted customers as inactive.

                **Allowed roles:** ADMIN, BRANCH_MANAGER
                **Side effects:** Sets isDeleted=true; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String customerNo) {
        service.deleteCustomer(customerNo);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }

    @DeleteMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @Operation(
        summary = "Soft-delete a customer by identity userId",
        description = """
                Same semantics as DELETE /customers/{customerNo}, but resolves the customer via the
                identity-service userId — useful for cascading deletes initiated from identity admin flows.

                **Allowed roles:** ADMIN, BRANCH_MANAGER
                **Side effects:** Sets isDeleted=true; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> deleteCustomerByUserId(@PathVariable String userId) {
        service.deleteCustomerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }

    @PutMapping("/{customerNo}/activate")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER')")
    @Operation(
        summary = "Activate a customer account",
        description = """
                Transitions the customer to ACTIVE status. Typically called after KYC approval and the
                first account application is approved.

                **Allowed roles:** CSR, BRANCH_MANAGER
                **Side effects:** Updates customer status to ACTIVE; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> activateCustomer(@PathVariable String customerNo) {
        service.activateCustomer(customerNo);
        return ResponseEntity.ok(ApiResponse.success("Customer account activated"));
    }

    @PutMapping("/{customerNo}/block")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER')")
    @Operation(
        summary = "Block a customer account",
        description = """
                Transitions the customer to BLOCKED status — usually because of fraud signals or
                compliance review. Blocked customers cannot transact via account-service.

                **Allowed roles:** CSR, BRANCH_MANAGER
                **Side effects:** Updates customer status to BLOCKED; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> blockCustomer(@PathVariable String customerNo) {
        service.blockCustomer(customerNo);
        return ResponseEntity.ok(ApiResponse.success("Customer account blocked"));
    }

    @PutMapping("/{customerNo}/deactivate")
    @PreAuthorize("hasAnyRole('CSR','BRANCH_MANAGER')")
    @Operation(
        summary = "Deactivate a customer account",
        description = """
                Transitions the customer to INACTIVE status — used for voluntary dormancy. The
                customer can be reactivated by a CSR later.

                **Allowed roles:** CSR, BRANCH_MANAGER
                **Side effects:** Updates customer status to INACTIVE; emits an audit event."""
    )
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerNo) {
        service.deactivateCustomer(customerNo);
        return ResponseEntity.ok(ApiResponse.success("Customer account deactivated"));
    }

    @GetMapping
    @Operation(
        summary = "List all customers",
        description = """
                Returns every non-deleted customer. Pagination is planned for a future iteration; for
                now the result set is unbounded — use the filter endpoints below for narrower queries.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllCustomers(), "Customers retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Filter customers by status",
        description = """
                Returns customers in the requested status (e.g. REGISTERED, ACTIVE, BLOCKED, INACTIVE).

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(service.getCustomersByStatus(status),
                "Customers with status [" + status + "] retrieved"));
    }

    @GetMapping("/branch/{branchCode}")
    @Operation(
        summary = "Filter customers by branch",
        description = """
                Returns customers assigned to the given branch — primarily used by branch-manager
                dashboards.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByBranch(@PathVariable String branchCode) {
        return ResponseEntity.ok(ApiResponse.success(service.getCustomersByBranch(branchCode),
                "Customers for branch [" + branchCode + "] retrieved"));
    }

    @GetMapping("/city/{city}")
    @Operation(
        summary = "Filter customers by city",
        description = """
                Returns customers whose registered address is in the given city.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(ApiResponse.success(service.getCustomersByCity(city),
                "Customers in city [" + city + "] retrieved"));
    }

    @PostMapping("/evaluate")
    @Operation(
        summary = "Evaluate loan eligibility for a customer",
        description = """
                Runs the in-house eligibility rule set against the customer's profile and the requested
                loan parameters, returning a pre-approval decision and indicative terms.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<LoanEligibilityResponse>> evaluate(@Valid @RequestBody LoanApplicationRequest request) {
        LoanEligibilityResponse result = service.evaluateLoan(request);
        return ResponseEntity.ok(ApiResponse.success(result,
                result.isEligible() ? "Loan pre-approved" : "Loan application rejected"));
    }
}

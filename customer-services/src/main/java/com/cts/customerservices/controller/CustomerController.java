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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customer lifecycles, statuses, and profiles")
public class CustomerController {

    private final CustomerService service;

    /**
     * Helper method to wrap data into a consistent ApiResponse format.
     */
    private <T> ResponseEntity<ApiResponse<T>> buildResponse(T data, String message, HttpStatus status) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, status);
    }

    @PostMapping
    @Operation(summary = "Register a new customer", description = "Creates a new customer record in the system and returns the saved details.")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> registerCustomer(@Valid @RequestBody CustomerRequestDTO request) {
        CustomerResponseDTO responseBody = service.registerCustomer(request);
        return buildResponse(responseBody, "Customer registered successfully", HttpStatus.CREATED);
    }

    @GetMapping("/{customerNo}")
    @Operation(summary = "Get customer by number", description = "Retrieves detailed information for a specific customer using their unique customer number.")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomer(@PathVariable String customerNo) {
        CustomerResponseDTO responseBody = service.getCustomer(customerNo);
        return buildResponse(responseBody, "Customer retrieved successfully", HttpStatus.OK);
    }

    @PutMapping("/{customerNo}")
    @Operation(summary = "Update customer details", description = "Updates the profile information for an existing customer identified by their customer number.")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
            @PathVariable String customerNo,
            @Valid @RequestBody CustomerRequestDTO request
    ) {
        CustomerResponseDTO responseBody = service.updateCustomer(customerNo, request);
        return buildResponse(responseBody, "Customer updated successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{customerNo}")
    @Operation(summary = "Soft delete customer", description = "Marks a customer as deleted in the system without removing the record from the database.")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String customerNo) {
        service.deleteCustomer(customerNo);
        return buildResponse(null, "Customer deleted successfully", HttpStatus.OK);
    }

    @PutMapping("/{customerNo}/activate")
    @Operation(summary = "Activate customer account", description = "Changes the customer status to ACTIVE, enabling their account services.")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(@PathVariable String customerNo) {
        service.activateCustomer(customerNo);
        return buildResponse(null, "Customer account activated", HttpStatus.OK);
    }

    @PutMapping("/{customerNo}/block")
    @Operation(summary = "Block customer account", description = "Sets the customer status to BLOCKED, restricting account access due to risk or security reasons.")
    public ResponseEntity<ApiResponse<Void>> blockCustomer(@PathVariable String customerNo) {
        service.blockCustomer(customerNo);
        return buildResponse(null, "Customer account blocked", HttpStatus.OK);
    }

    @PutMapping("/{customerNo}/deactivate")
    @Operation(summary = "Deactivate customer account", description = "Sets the customer status to INACTIVE, typically used for account closures.")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerNo) {
        service.deactivateCustomer(customerNo);
        return buildResponse(null, "Customer account deactivated", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Fetch all customers", description = "Retrieves a list of all customers registered in the system.")
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getAllCustomers() {
        List<CustomerResponseDTO> responseBody = service.getAllCustomers();
        return buildResponse(responseBody, "All customers retrieved successfully", HttpStatus.OK);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter customers by status", description = "Retrieves a list of customers based on their current status (e.g., ACTIVE, BLOCKED).")
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByStatus(@PathVariable String status) {
        List<CustomerResponseDTO> responseBody = service.getCustomersByStatus(status);
        return buildResponse(responseBody, "Customers with status [" + status + "] retrieved", HttpStatus.OK);
    }

    @GetMapping("/branch/{branchCode}")
    @Operation(summary = "Filter customers by branch", description = "Retrieves all customers associated with a specific bank branch code.")
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByBranch(@PathVariable String branchCode) {
        List<CustomerResponseDTO> responseBody = service.getCustomersByBranch(branchCode);
        return buildResponse(responseBody, "Customers for branch [" + branchCode + "] retrieved", HttpStatus.OK);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Filter customers by city", description = "Retrieves a list of customers residing in a specific city.")
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getByCity(@PathVariable String city) {
        List<CustomerResponseDTO> responseBody = service.getCustomersByCity(city);
        return buildResponse(responseBody, "Customers in city [" + city + "] retrieved", HttpStatus.OK);
    }
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate loan eligibility", description = "Checks age, KYC, and performs EMI vs Income calculation.")
    public ResponseEntity<ApiResponse<LoanEligibilityResponse>> evaluate(@Valid @RequestBody LoanApplicationRequest request) {
        LoanEligibilityResponse data = service.evaluateLoan(request);

        return ResponseEntity.ok(
                ApiResponse.<LoanEligibilityResponse>builder()
                        .status("SUCCESS")
                        .message(data.isEligible() ? "Loan Pre-Approved" : "Loan Application Rejected")
                        .data(data)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
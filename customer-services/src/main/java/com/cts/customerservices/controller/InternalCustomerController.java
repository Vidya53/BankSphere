package com.cts.customerservices.controller;

import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.service.CustomerService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal Customer Service Controller
 *
 * These endpoints are designed for inter-service communication via Feign clients.
 * Used by: account-service, loan-service
 * Should NOT be exposed through the API Gateway.
 */
@RestController
@RequestMapping("/api/v1/customers/internal")
@RequiredArgsConstructor
@Hidden
@Tag(name = "Customers - Internal", description = "Internal inter-service endpoints (Hidden from API docs)")
public class InternalCustomerController {

    private final CustomerService customerService;

    /**
     * Get customer details - Internal endpoint for Feign clients
     */
    @GetMapping("/{customerNo}")
    @Operation(summary = "Get customer details (Internal)", hidden = true)
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable String customerNo) {
        CustomerResponseDTO customer = customerService.getCustomer(customerNo);
        return ResponseEntity.ok(customer);
    }

    /**
     * Check if customer is active - Internal endpoint for Feign clients
     */
    @GetMapping("/{customerNo}/status")
    @Operation(summary = "Check customer status (Internal)", hidden = true)
    public ResponseEntity<Boolean> isCustomerActive(@PathVariable String customerNo) {
        try {
            CustomerResponseDTO customer = customerService.getCustomer(customerNo);
            boolean isActive = "ACTIVE".equalsIgnoreCase(customer.getStatus());
            return ResponseEntity.ok(isActive);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Check loan eligibility - Internal endpoint for Loan Service
     */
    @PostMapping("/check-eligibility")
    @Operation(summary = "Check loan eligibility (Internal)", hidden = true)
    public ResponseEntity<Boolean> checkLoanEligibility(@RequestBody EligibilityRequest request) {
        try {
            CustomerResponseDTO customer = customerService.getCustomer(request.getCustomerId());

            // Check age constraints
            if (customer.getAge() < request.getMinAge() || customer.getAge() > request.getMaxAge()) {
                return ResponseEntity.ok(false);
            }

            // Check KYC status
            if (!"APPROVED".equalsIgnoreCase(customer.getKycStatus())) {
                return ResponseEntity.ok(false);
            }

            // Check customer status
            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
                return ResponseEntity.ok(false);
            }

            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * DTO for eligibility request
     */
    @lombok.Data
    public static class EligibilityRequest {
        private String customerId;
        private int minAge = 21;
        private int maxAge = 65;
    }
}


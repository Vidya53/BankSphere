package com.cts.customerservices.controller;

import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.KycStatus;
import com.cts.customerservices.repository.CustomerRepository;
import com.cts.customerservices.repository.KycRepository;
import com.cts.customerservices.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/internal/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal · Customer Lookup", description = "Internal-only customer state queries consumed by other services via Feign")
public class InternalCustomerController {

    private final CustomerRepository customerRepository;
    private final KycRepository kycRepository;
    private final CustomerService customerService;

    @GetMapping("/{userId}/kyc-approved")
    @Operation(
        summary = "Check whether the customer for an identity userId has APPROVED KYC",
        description = """
                Returns true only when a customer profile exists for the userId and its linked KYC record
                is in APPROVED status. Used by account-service and loan-service as a pre-flight gate.

                **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<Boolean> isKycApproved(@PathVariable String userId) {
        Optional<Customer> customer = customerRepository.findByUserId(userId);
        if (customer.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        Optional<Kyc> kyc = kycRepository.findByCustomerNo(customer.get().getCustomerNo());
        boolean approved = kyc.isPresent() && kyc.get().getStatus() == KycStatus.APPROVED;
        return ResponseEntity.ok(approved);
    }

    /**
     * Returns true when the customer linked to this identity userId is in ACTIVE
     * state and not soft-deleted. Used by account-service to gate transfers and
     * by loan-service to gate disbursements.
     */
    @GetMapping("/{userId}/active")
    @Operation(
        summary = "Check whether the customer for an identity userId is ACTIVE and not soft-deleted",
        description = """
                Returns true only when the customer exists, has isDeleted=false, and is in ACTIVE status.
                Used by account-service to gate transfers and by loan-service to gate disbursements.

                **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign."""
    )
    public ResponseEntity<Boolean> isCustomerActive(@PathVariable String userId) {
        Optional<Customer> customer = customerRepository.findByUserId(userId);
        if (customer.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        Customer c = customer.get();
        boolean active = !Boolean.TRUE.equals(c.getIsDeleted())
                && c.getStatus() == CustomerStatus.ACTIVE;
        return ResponseEntity.ok(active);
    }

    /**
     * Idempotently promotes a REGISTERED customer to ACTIVE. Called by
     * account-service after a customer's first account application is
     * approved (which itself requires KYC approval). Re-activation of
     * BLOCKED/INACTIVE customers still has to go through the staff console.
     *
     * Returns true if the customer is ACTIVE after this call, false if
     * the request was a no-op (customer missing, soft-deleted, or in a
     * non-REGISTERED non-terminal state).
     */
    @PostMapping("/{userId}/activate")
    @Operation(
        summary = "Idempotently promote a REGISTERED customer to ACTIVE",
        description = """
                Called by account-service after a customer's first account application is approved
                (which itself requires KYC approval). Returns true if the customer ends up ACTIVE; returns
                false (no-op) when the customer is missing, soft-deleted, or in a non-REGISTERED state
                that requires staff review (BLOCKED, INACTIVE).

                **Allowed roles:** Internal — blocked at the API Gateway; callable only by other services via Feign.
                **Side effects:** Transitions status to ACTIVE; emits an audit event."""
    )
    public ResponseEntity<Boolean> activateByUserId(@PathVariable String userId) {
        Optional<Customer> opt = customerRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            log.warn("Auto-activate skipped — no customer profile for userId {}", userId);
            return ResponseEntity.ok(false);
        }
        Customer c = opt.get();
        if (Boolean.TRUE.equals(c.getIsDeleted())) {
            log.warn("Auto-activate skipped — customer {} is soft-deleted", c.getCustomerNo());
            return ResponseEntity.ok(false);
        }
        if (c.getStatus() == CustomerStatus.ACTIVE) {
            return ResponseEntity.ok(true);
        }
        if (c.getStatus() != CustomerStatus.REGISTERED) {
            log.info("Auto-activate skipped for customer {} — current status {} requires staff review",
                    c.getCustomerNo(), c.getStatus());
            return ResponseEntity.ok(false);
        }
        customerService.activateCustomer(c.getCustomerNo());
        log.info("Customer {} (userId={}) auto-activated", c.getCustomerNo(), userId);
        return ResponseEntity.ok(true);
    }
}

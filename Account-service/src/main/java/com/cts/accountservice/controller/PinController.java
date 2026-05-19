package com.cts.accountservice.controller;

import com.cts.accountservice.dto.response.ApiResponse;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.PinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts/{accountNo}/pin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Transaction PIN", description = "Customer-managed transaction PIN for an account (BCrypt-hashed, lock-out enforced)")
public class PinController {

    private final PinService pinService;
    private final UserContextExtractor userContextExtractor;

    @GetMapping("/status")
    @Operation(
            summary = "Check PIN status",
            description = """
                    Returns `{isSet: true|false}` indicating whether the calling customer has already configured a transaction PIN for the given account.

                    **Allowed roles:** CUSTOMER"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @PathVariable String accountNo,
            HttpServletRequest request) {
        UserContext ctx = userContextExtractor.extract(request);
        boolean isSet = pinService.isPinSet(accountNo, ctx.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                isSet ? "PIN is set" : "PIN not set yet",
                Map.of("isSet", isSet)));
    }

    @PostMapping("/set")
    @Operation(
            summary = "Set the initial transaction PIN",
            description = """
                    Sets the first transaction PIN for the account; fails if a PIN is already configured. PIN must be 4–6 digits and is stored as a BCrypt hash.

                    **Allowed roles:** CUSTOMER
                    **Side effects:** Emits an audit event on `banking.audit.events`."""
    )
    public ResponseEntity<ApiResponse<Void>> set(
            @PathVariable String accountNo,
            @Valid @RequestBody SetPinRequest request,
            HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        pinService.setInitialPin(accountNo, ctx.getUserId(), request.getPin());
        return ResponseEntity.ok(ApiResponse.success("PIN set successfully", null));
    }

    @PostMapping("/change")
    @Operation(
            summary = "Change an existing transaction PIN",
            description = """
                    Verifies the current PIN before persisting the new one. 5 consecutive wrong attempts lock the PIN for 15 minutes.

                    **Allowed roles:** CUSTOMER
                    **Side effects:** Emits an audit event on `banking.audit.events`; increments lock-out counter on failure."""
    )
    public ResponseEntity<ApiResponse<Void>> change(
            @PathVariable String accountNo,
            @Valid @RequestBody ChangePinRequest request,
            HttpServletRequest http) {
        UserContext ctx = userContextExtractor.extract(http);
        pinService.changePin(accountNo, ctx.getUserId(), request.getCurrentPin(), request.getNewPin());
        return ResponseEntity.ok(ApiResponse.success("PIN updated successfully", null));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor
    public static class SetPinRequest {
        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4 to 6 digits")
        private String pin;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ChangePinRequest {
        @NotBlank(message = "Current PIN is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "Current PIN must be 4 to 6 digits")
        private String currentPin;

        @NotBlank(message = "New PIN is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "New PIN must be 4 to 6 digits")
        private String newPin;
    }
}

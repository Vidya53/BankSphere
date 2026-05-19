package com.cts.accountservice.exception;

import com.cts.accountservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit test for {@link GlobalExceptionHandler}.
 *
 * No MockMvc, no Spring context — we invoke each handler method ourselves,
 * pass in real (or mock) exceptions, and assert the {@link ResponseEntity}
 * status and body. The assertions guard the public error contract: status,
 * envelope shape, and message vs detail separation.
 */
@DisplayName("GlobalExceptionHandler — error envelope contract")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setRequestURI("/api/v1/test");
        request = mock;
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 with the exception message as detail")
    void resourceNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(
                new ResourceNotFoundException("nothing here"), request);

        assertOk(resp, HttpStatus.NOT_FOUND, "Resource not found", "nothing here");
    }

    @Test
    @DisplayName("AccountNotActiveException → 422 Unprocessable Entity")
    void accountNotActive() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccountNotActive(
                new AccountNotActiveException("ACC1 is FROZEN"), request);

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "Account not active", "ACC1 is FROZEN");
    }

    @Test
    @DisplayName("InsufficientBalanceException → 422 with the funds-short message")
    void insufficientBalance() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleInsufficientBalance(
                new InsufficientBalanceException("Balance ₹100 < required ₹500"), request);

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance",
                "Balance ₹100 < required ₹500");
    }

    @Test
    @DisplayName("KycNotApprovedException → 422")
    void kycNotApproved() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleKycNotApproved(
                new KycNotApprovedException("KYC pending"), request);

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "KYC not approved", "KYC pending");
    }

    @Test
    @DisplayName("DuplicateApplicationException → 409 Conflict")
    void duplicateApplication() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleDuplicateApplication(
                new DuplicateApplicationException("already have SAVINGS pending"), request);

        assertOk(resp, HttpStatus.CONFLICT, "Duplicate application", "already have SAVINGS pending");
    }

    @Test
    @DisplayName("UnauthorizedBranchAccessException → 403 Forbidden")
    void unauthorizedBranch() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnauthorizedBranch(
                new UnauthorizedBranchAccessException("Wrong branch"), request);

        assertOk(resp, HttpStatus.FORBIDDEN, "Unauthorized branch access", "Wrong branch");
    }

    @Test
    @DisplayName("MissingGatewayHeaderException → 401 Unauthorized")
    void missingGatewayHeader() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingGatewayHeader(
                new MissingGatewayHeaderException("X-User-Id missing"), request);

        assertOk(resp, HttpStatus.UNAUTHORIZED, "Unauthorized — missing identity context",
                "X-User-Id missing");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with all field errors concatenated")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "branchCode", "Branch code is required"));
        bindResult.addError(new FieldError("obj", "initialDeposit", "Cannot be negative"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().getError())
                .contains("branchCode: Branch code is required")
                .contains("initialDeposit: Cannot be negative");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 (path / query parameter violations)")
    void constraintViolation() {
        // We can't easily fabricate a ConstraintViolation in isolation, so
        // pass an empty set — the handler must still produce a clean 400.
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with a friendly message")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 (Spring Security @PreAuthorize denial)")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertOk(resp, HttpStatus.FORBIDDEN, "Access denied",
                "Your role is not permitted to perform this action.");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400 (e.g. non-numeric path id)")
    void typeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleTypeMismatch(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getError()).contains("abc").contains("id");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400")
    void missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("status", "String");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Missing required parameter");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 with a generic detail (no SQL leakage)")
    void dataIntegrity() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("UK_accountNo violated");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getError())
                .doesNotContainIgnoringCase("UK_accountNo")
                .contains("already exists");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArg(
                new IllegalArgumentException("amount must be positive"), request);

        assertOk(resp, HttpStatus.BAD_REQUEST, "Invalid argument", "amount must be positive");
    }

    @Test
    @DisplayName("Unknown exception → 500 with a generic message (no stack leakage)")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        // We deliberately don't leak "boom" to the client.
        assertThat(resp.getBody().getError()).doesNotContain("boom");
    }

    // ────────────────────────────────────────────────────────────────────────
    private void assertOk(ResponseEntity<ApiResponse<Void>> resp,
                          HttpStatus status, String message, String detail) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getStatusCode()).isEqualTo(status.value());
        assertThat(resp.getBody().getMessage()).isEqualTo(message);
        assertThat(resp.getBody().getError()).isEqualTo(detail);
        assertThat(resp.getBody().getTimestamp()).isNotNull();
    }
}

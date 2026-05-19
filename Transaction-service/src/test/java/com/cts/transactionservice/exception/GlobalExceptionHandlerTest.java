package com.cts.transactionservice.exception;

import com.cts.transactionservice.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
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
 * status and body envelope.
 */
@DisplayName("GlobalExceptionHandler — error envelope contract")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("TransactionNotFoundException → 404")
    void transactionNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleTransactionNotFound(
                new TransactionNotFoundException("Transaction not found: tx-1"));

        assertOk(resp, HttpStatus.NOT_FOUND, "Transaction not found", "Transaction not found: tx-1");
    }

    @Test
    @DisplayName("DuplicateTransactionException → 409 Conflict")
    void duplicateTransaction() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleDuplicateTransaction(
                new DuplicateTransactionException("dup key xyz"));

        assertOk(resp, HttpStatus.CONFLICT, "Duplicate transaction", "dup key xyz");
    }

    @Test
    @DisplayName("InvalidTransactionException → 422")
    void invalidTransaction() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleInvalidTransaction(
                new InvalidTransactionException("Bad payload"));

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "Invalid transaction", "Bad payload");
    }

    @Test
    @DisplayName("IllegalTransactionStateException → 409 Conflict")
    void illegalState() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalTransactionState(
                new IllegalTransactionStateException("Cannot cancel SUCCESS"));

        assertOk(resp, HttpStatus.CONFLICT, "Illegal state transition", "Cannot cancel SUCCESS");
    }

    @Test
    @DisplayName("BadRequestException → 400")
    void badRequest() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBadRequest(
                new BadRequestException("malformed input"));

        assertOk(resp, HttpStatus.BAD_REQUEST, "Bad request", "malformed input");
    }

    @Test
    @DisplayName("ExternalServiceException → 503")
    void externalService() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleExternalService(
                new ExternalServiceException("account-service down"));

        assertOk(resp, HttpStatus.SERVICE_UNAVAILABLE, "External service unavailable", "account-service down");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors concatenated")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "amount", "must be positive"));
        bindResult.addError(new FieldError("obj", "currency", "is required"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidationErrors(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().getError()).contains("must be positive").contains("is required");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with friendly message")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"));

        assertOk(resp, HttpStatus.FORBIDDEN, "Access denied",
                "Your role is not permitted to perform this action.");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400")
    void typeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleTypeMismatch(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getError()).contains("abc").contains("id");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400")
    void missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("status", "String");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Missing parameter");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 with generic detail")
    void dataIntegrity() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("UK_idempotency violated");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrityViolation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getError())
                .doesNotContainIgnoringCase("UK_idempotency")
                .contains("already exists");
    }

    @Test
    @DisplayName("OptimisticLockingFailureException → 409 Conflict")
    void optimisticLocking() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleOptimisticLocking(
                new OptimisticLockingFailureException("version mismatch"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getMessage()).isEqualTo("Concurrent modification");
    }

    @Test
    @DisplayName("Unknown exception → 500 with generic message (no stack leakage)")
    void generic() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAllUnexpected(
                new RuntimeException("boom"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).isEqualTo("An unexpected error occurred");
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

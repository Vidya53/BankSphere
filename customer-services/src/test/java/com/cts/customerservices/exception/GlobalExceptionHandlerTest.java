package com.cts.customerservices.exception;

import com.cts.customerservices.payload.ApiResponse;
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
        mock.setRequestURI("/api/v1/customers");
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
    @DisplayName("CustomerAlreadyExistsException → 409 Conflict")
    void alreadyExists() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAlreadyExists(
                new CustomerAlreadyExistsException("duplicate email"), request);

        assertOk(resp, HttpStatus.CONFLICT, "Customer already exists", "duplicate email");
    }

    @Test
    @DisplayName("DuplicateKycException → 409 Conflict")
    void duplicateKyc() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleDuplicateKyc(
                new DuplicateKycException("CUST-ABC"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getMessage()).isEqualTo("Duplicate KYC submission");
    }

    @Test
    @DisplayName("BusinessException → 422 Unprocessable Entity")
    void business() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusiness(
                new BusinessException("rule violated"), request);

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", "rule violated");
    }

    @Test
    @DisplayName("InvalidStatusTransitionException → 422")
    void invalidTransition() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleInvalidTransition(
                new InvalidStatusTransitionException("CLOSED", "ACTIVE"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().getMessage()).isEqualTo("Invalid status transition");
    }

    @Test
    @DisplayName("KycNotVerifiedException → 422")
    void kycNotVerified() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleKycNotVerified(
                new KycNotVerifiedException("CUST-ABC"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().getMessage()).isEqualTo("KYC not verified");
    }

    @Test
    @DisplayName("LoanEligibilityException → 422")
    void loanEligibility() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleLoanEligibility(
                new LoanEligibilityException("not eligible"), request);

        assertOk(resp, HttpStatus.UNPROCESSABLE_ENTITY, "Loan ineligible", "not eligible");
    }

    @Test
    @DisplayName("CustomerNotActiveException → 403 Forbidden")
    void notActive() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotActive(
                new CustomerNotActiveException("CUST-ABC"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).isEqualTo("Customer not active");
    }

    @Test
    @DisplayName("CustomerDeletedException → 410 Gone")
    void deleted() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleDeleted(
                new CustomerDeletedException("CUST-ABC"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(resp.getBody().getMessage()).isEqualTo("Customer record deleted");
    }

    @Test
    @DisplayName("BranchNotActiveException → 400")
    void branchInactive() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBranchNotActive(
                new BranchNotActiveException("BR404"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Branch not active");
        assertThat(resp.getBody().getError()).contains("BR404");
    }

    @Test
    @DisplayName("InvalidDocumentException → 400")
    void invalidDocument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleInvalidDocument(
                new InvalidDocumentException("bad PAN"), request);

        assertOk(resp, HttpStatus.BAD_REQUEST, "Invalid document", "bad PAN");
    }

    @Test
    @DisplayName("ServiceUnavailableException → 503")
    void serviceUnavailable() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleServiceUnavailable(
                new ServiceUnavailableException("Branch Service"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody().getMessage()).isEqualTo("Downstream service unavailable");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with all field errors concatenated")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "email", "Email is required"));
        bindResult.addError(new FieldError("obj", "mobileNumber", "Invalid mobile"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().getError())
                .contains("email: Email is required")
                .contains("mobileNumber: Invalid mobile");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 (path / query parameter violations)")
    void constraintViolation() {
        // We can't easily fabricate a ConstraintViolation in isolation, so
        // pass an empty set — the handler must still produce a clean 400.
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleEntityValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 with a friendly message")
    void dataIntegrity() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate entry 'foo' for key 'email'");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getMessage()).isEqualTo("A record with this value already exists");
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
    @DisplayName("MethodArgumentTypeMismatchException → 400")
    void typeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("customerNo");
        when(ex.getValue()).thenReturn("xyz");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleTypeMismatch(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getError()).contains("xyz").contains("customerNo");
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
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArg(
                new IllegalArgumentException("bad value"), request);

        assertOk(resp, HttpStatus.BAD_REQUEST, "Invalid argument", "bad value");
    }

    @Test
    @DisplayName("Unknown exception → 500 with a generic message (no stack leakage)")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(
                new RuntimeException("boom"), request);

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

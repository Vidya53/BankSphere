package com.cts.loanservice.exception;

import com.cts.loanservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
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
 * status and body envelope.
 */
@DisplayName("GlobalExceptionHandler — error envelope contract")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setRequestURI("/loans/test");
        request = mock;
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404")
    void resourceNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(
                new ResourceNotFoundException("Loan not found with ID: 1"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getMessage()).contains("Loan not found");
    }

    @Test
    @DisplayName("BusinessException → 422 Unprocessable Entity")
    void businessException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBusiness(
                new BusinessException("Loan already processed"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().getMessage()).isEqualTo("Loan already processed");
    }

    @Test
    @DisplayName("ValidationException → 400 Bad Request")
    void validationException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidationException(
                new ValidationException("Bad data"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Bad data");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field-error map")
    void methodArgumentNotValid() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "amount", "must be positive"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Object>> resp = handler.handleMethodValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().getErrors().toString()).contains("amount").contains("must be positive");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400")
    void typeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleTypeMismatch(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("abc").contains("id");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400")
    void missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("status", "String");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArg(
                new IllegalArgumentException("bad"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("bad");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 (path / query parameter violations)")
    void constraintViolation() {
        // We can't easily fabricate a ConstraintViolation in isolation, so
        // pass an empty set — the handler must still produce a clean 400.
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with friendly message")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("Malformed");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 (Spring Security @PreAuthorize denial)")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).contains("not permitted");
    }

    @Test
    @DisplayName("Unknown exception → 500 with generic message (no stack leakage)")
    void generic() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // We deliberately don't leak "boom" to the client.
        assertThat(resp.getBody().getMessage()).doesNotContain("boom");
    }
}

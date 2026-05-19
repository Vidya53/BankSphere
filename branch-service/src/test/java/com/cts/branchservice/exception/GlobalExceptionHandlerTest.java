package com.cts.branchservice.exception;

import com.cts.branchservice.util.ApiResponse;
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
 * status and body.
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
    @DisplayName("BranchNotFoundException → 404")
    void branchNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBranchNotFound(
                new BranchNotFoundException("BR404 not found"), request);

        assertFailure(resp, HttpStatus.NOT_FOUND, "BR404 not found");
    }

    @Test
    @DisplayName("BranchAlreadyExistsException → 409")
    void branchAlreadyExists() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBranchAlreadyExists(
                new BranchAlreadyExistsException("BR001 exists"), request);

        assertFailure(resp, HttpStatus.CONFLICT, "BR001 exists");
    }

    @Test
    @DisplayName("BranchInactiveException → 422")
    void branchInactive() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBranchInactive(
                new BranchInactiveException("BR1 inactive"), request);

        assertFailure(resp, HttpStatus.UNPROCESSABLE_ENTITY, "BR1 inactive");
    }

    @Test
    @DisplayName("EmployeeNotFoundException → 404")
    void employeeNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleEmployeeNotFound(
                new EmployeeNotFoundException("EMP404 not found"), request);

        assertFailure(resp, HttpStatus.NOT_FOUND, "EMP404 not found");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors map")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "branchCode", "Branch code is required"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Object>> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("Validation failed");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with friendly message")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).contains("Malformed request body");
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).contains("Access denied");
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
                new MissingServletRequestParameterException("q", "String");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegal(
                new IllegalArgumentException("bad arg"), request);

        assertFailure(resp, HttpStatus.BAD_REQUEST, "bad arg");
    }

    @Test
    @DisplayName("Unknown exception → 500")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).contains("unexpected");
    }

    private void assertFailure(ResponseEntity<ApiResponse<Void>> resp,
                                HttpStatus status, String message) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getStatusCode()).isEqualTo(status.value());
        assertThat(resp.getBody().getMessage()).isEqualTo(message);
    }
}

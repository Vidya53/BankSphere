package com.cts.auditservice.exception;

import com.cts.auditservice.util.ApiResponse;
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

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Direct unit test for {@link GlobalExceptionHandler}.
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
    @DisplayName("AuditLogNotFoundException → 404")
    void notFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(
                new AuditLogNotFoundException("not found"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getMessage()).isEqualTo("not found");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors map")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "field1", "must not be blank"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Map<String, String>>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(resp.getBody().getErrors()).isNotNull();
    }

    @Test
    @DisplayName("ConstraintViolationException → 400")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with friendly message")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArg(
                new IllegalArgumentException("bad arg"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getMessage()).isEqualTo("bad arg");
    }

    @Test
    @DisplayName("Unknown exception → 500")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getMessage()).contains("unexpected");
    }
}

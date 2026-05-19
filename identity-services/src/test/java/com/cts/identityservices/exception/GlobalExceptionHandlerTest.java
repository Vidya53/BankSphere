package com.cts.identityservices.exception;

import com.cts.identityservices.dto.response.ApiResponse;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Direct unit test for {@link GlobalExceptionHandler}.
 *
 * No MockMvc, no Spring context — we invoke each handler method ourselves,
 * pass in real (or mock) exceptions, and assert the {@link ResponseEntity}
 * status and body. The assertions guard the public error contract: status,
 * envelope shape, and message vs error separation.
 */
@DisplayName("GlobalExceptionHandler — error envelope contract")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setRequestURI("/auth/test");
        request = mock;
    }

    @Test
    @DisplayName("UserAlreadyExistsException → 409 Conflict")
    void userAlreadyExists() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleUserExists(
                new UserAlreadyExistsException("email taken"), request);

        assertEnvelope(resp, HttpStatus.CONFLICT, "User already exists");
        assertThat(resp.getBody().getError()).contains("CONFLICT").contains("email taken");
    }

    @Test
    @DisplayName("UserNotFoundException → 404 Not Found")
    void userNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNotFound(
                new UserNotFoundException("no user"), request);

        assertEnvelope(resp, HttpStatus.NOT_FOUND, "User not found");
        assertThat(resp.getBody().getError()).contains("USER_NOT_FOUND").contains("no user");
    }

    @Test
    @DisplayName("InvalidCredentialsException → 401 Unauthorized")
    void invalidCredentials() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Invalid email or password"), request);

        assertEnvelope(resp, HttpStatus.UNAUTHORIZED, "Authentication failed");
        assertThat(resp.getBody().getError()).contains("AUTH_FAILED");
    }

    @Test
    @DisplayName("RefreshTokenException (not-found) → 401")
    void refreshTokenNotFound() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleRefreshToken(
                RefreshTokenException.notFound(), request);

        assertEnvelope(resp, HttpStatus.UNAUTHORIZED, "Token error");
        assertThat(resp.getBody().getError()).contains("REFRESH_TOKEN_INVALID");
    }

    @Test
    @DisplayName("RefreshTokenException (expired) → 401")
    void refreshTokenExpired() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleRefreshToken(
                RefreshTokenException.expired(), request);

        assertEnvelope(resp, HttpStatus.UNAUTHORIZED, "Token error");
        assertThat(resp.getBody().getError()).contains("REFRESH_TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 Bad Request")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleIllegalArgument(
                new IllegalArgumentException("Role is required"), request);

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, "Bad request");
        assertThat(resp.getBody().getError()).contains("BAD_REQUEST").contains("Role is required");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors concatenated")
    void validation() {
        BeanPropertyBindingResult bindResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindResult.addError(new FieldError("obj", "email", "Email is required"));
        bindResult.addError(new FieldError("obj", "password", "Password must be at least 6 characters"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindResult);

        ResponseEntity<ApiResponse<Void>> resp = handler.handleValidation(ex, request);

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, "Validation failed");
        assertThat(resp.getBody().getError())
                .contains("VALIDATION_FAILED")
                .contains("email: Email is required")
                .contains("password: Password must be at least 6 characters");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 (path / query parameter violations)")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of());

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex, request);

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, "Validation failed");
        assertThat(resp.getBody().getError()).contains("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("DataIntegrityViolationException with duplicate email → friendly 409")
    void dataIntegrityDuplicateEmail() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate entry 'a@b.com' for key 'users.email'");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertEnvelope(resp, HttpStatus.CONFLICT, "Email already registered");
        assertThat(resp.getBody().getError()).contains("CONFLICT");
    }

    @Test
    @DisplayName("DataIntegrityViolationException with duplicate phone → friendly 409")
    void dataIntegrityDuplicatePhone() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate entry '9876543210' for key 'users.phone_number'");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertEnvelope(resp, HttpStatus.CONFLICT, "Phone number already registered");
    }

    @Test
    @DisplayName("DataIntegrityViolationException with generic duplicate → fallback 409 message")
    void dataIntegrityGenericDuplicate() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate entry 'xyz' for key 'some_index'");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertEnvelope(resp, HttpStatus.CONFLICT, "A user with that value already exists");
    }

    @Test
    @DisplayName("DataIntegrityViolationException without 'duplicate' → fallback message")
    void dataIntegrityOther() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("constraint violated");

        ResponseEntity<ApiResponse<Void>> resp = handler.handleDataIntegrity(ex, request);

        assertEnvelope(resp, HttpStatus.CONFLICT, "Data integrity violation");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 with friendly hint")
    void unreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, "Malformed request body");
        assertThat(resp.getBody().getError()).contains("MALFORMED_BODY");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException with Role enum cause → role-specific hint")
    void unreadableRoleEnum() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Cannot deserialize value of type Role from String \"BAD\"",
                new MockHttpInputMessage("garbage".getBytes()));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleUnreadable(ex, request);

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, "Malformed request body");
        assertThat(resp.getBody().getError()).contains("Role must be one of");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 Forbidden (Spring Security @PreAuthorize denial)")
    void accessDenied() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertEnvelope(resp, HttpStatus.FORBIDDEN, "Access denied");
        assertThat(resp.getBody().getError())
                .contains("ACCESS_DENIED")
                .contains("not permitted");
    }

    @Test
    @DisplayName("Unknown exception → 500 with generic message (no stack leakage)")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertEnvelope(resp, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        assertThat(resp.getBody().getError())
                .contains("INTERNAL_ERROR")
                .doesNotContain("boom");
    }

    // ────────────────────────────────────────────────────────────────────────
    private void assertEnvelope(ResponseEntity<ApiResponse<Void>> resp,
                                HttpStatus status, String message) {
        assertThat(resp.getStatusCode()).isEqualTo(status);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getStatusCode()).isEqualTo(status.value());
        assertThat(resp.getBody().getMessage()).isEqualTo(message);
        assertThat(resp.getBody().getError()).isNotNull();
    }
}

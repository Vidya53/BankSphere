package com.cts.identityservices.exception;

import com.cts.identityservices.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserExists(UserAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("User already exists: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.CONFLICT, "CONFLICT", "User already exists", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UserNotFoundException ex, HttpServletRequest req) {
        log.warn("User not found: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        log.warn("Authentication failed: {} | path={}", ex.getMessage(), req.getRequestURI());
        // Deliberate: 401 for both wrong password and blocked/suspended — don't leak account state
        return buildError(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshToken(RefreshTokenException ex, HttpServletRequest req) {
        boolean isExpiry = "REFRESH_TOKEN_EXPIRED".equals(ex.getErrorCode());
        log.warn("Refresh token error [{}]: {} | path={}", ex.getErrorCode(), ex.getMessage(), req.getRequestURI());
        HttpStatus status = isExpiry ? HttpStatus.UNAUTHORIZED : HttpStatus.UNAUTHORIZED;
        return buildError(status, ex.getErrorCode(), "Token error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Bad request: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Bad request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> {
                    String prop = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
                    String name = prop.contains(".") ? prop.substring(prop.lastIndexOf('.') + 1) : prop;
                    return name + ": " + v.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("Entity validation failed: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation: {} | path={}", root, req.getRequestURI());
        // Map common unique-constraint failures to a friendly conflict message
        String friendly;
        if (root != null && root.toLowerCase().contains("duplicate")) {
            if (root.toLowerCase().contains("email"))                friendly = "Email already registered";
            else if (root.toLowerCase().contains("phone"))            friendly = "Phone number already registered";
            else                                                     friendly = "A user with that value already exists";
        } else {
            friendly = "Data integrity violation";
        }
        return buildError(HttpStatus.CONFLICT, "CONFLICT", friendly, root);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed request body: {} | path={}", ex.getMessage(), req.getRequestURI());
        String hint = ex.getMessage() != null && ex.getMessage().contains("Role")
                ? "Role must be one of: CSR, LOAN_OFFICER, BRANCH_MANAGER, ADMIN."
                : "Check field types, dates and enum values.";
        return buildError(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Malformed request body", hint);
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied",
                "Your role is not permitted to perform this action.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", "Please contact support if the problem persists.");
    }

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String errorCode,
                                                          String message, String detail) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .error("[" + errorCode + "] " + detail)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

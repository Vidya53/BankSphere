package com.cts.accountservice.exception;

import com.cts.accountservice.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotActive(AccountNotActiveException ex, HttpServletRequest req) {
        log.warn("Account not active: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Account not active", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest req) {
        log.warn("Insufficient balance: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance", ex.getMessage());
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest req) {
        log.warn("Invalid operation: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid operation", ex.getMessage());
    }

    @ExceptionHandler(KycNotApprovedException.class)
    public ResponseEntity<ApiResponse<Void>> handleKycNotApproved(KycNotApprovedException ex, HttpServletRequest req) {
        log.warn("KYC not approved: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "KYC not approved", ex.getMessage());
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateApplication(DuplicateApplicationException ex, HttpServletRequest req) {
        log.warn("Duplicate application: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.CONFLICT, "Duplicate application", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedBranchAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedBranch(UnauthorizedBranchAccessException ex, HttpServletRequest req) {
        log.warn("Unauthorized branch access: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "Unauthorized branch access", ex.getMessage());
    }

    @ExceptionHandler(MissingGatewayHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingGatewayHeader(MissingGatewayHeaderException ex, HttpServletRequest req) {
        log.warn("Missing gateway header: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNAUTHORIZED, "Unauthorized — missing identity context", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    /** Bean-validation failures on path / query / single-arg parameters (e.g. @Min on a @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    /** Malformed JSON body, unknown enum value, type-incompatible primitives in payloads. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Malformed request body: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body",
                "The request body could not be parsed. Check JSON syntax, enum values, and field types.");
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "Access denied",
                "Your role is not permitted to perform this action.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String detail = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Invalid parameter type", detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        log.warn("Missing parameter: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Missing required parameter", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Data integrity violation at {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT, "Data conflict",
                "A record with the same unique identifier already exists.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Invalid argument", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                "Please contact support if the problem persists.");
    }

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String message, String detail) {
        return ResponseEntity.status(status).body(ApiResponse.error(status, message, detail));
    }
}

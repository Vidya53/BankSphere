package com.cts.branchservice.exception;

import com.cts.branchservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBranchNotFound(BranchNotFoundException ex, HttpServletRequest req) {
        log.warn("Branch not found: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BranchAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBranchAlreadyExists(BranchAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("Branch conflict: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BranchInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleBranchInactive(BranchInactiveException ex, HttpServletRequest req) {
        log.warn("Branch inactive: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmployeeNotFound(EmployeeNotFoundException ex, HttpServletRequest req) {
        log.warn("Employee not found: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = ((FieldError) e).getField();
            errors.put(field, e.getDefaultMessage());
        });
        log.warn("Validation failed: {} | path={}", errors, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, "Validation failed", errors));
    }

    /** Bean-validation failures on path / query / single-arg parameters (e.g. @Min on a @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed: " + detail);
    }

    /** Malformed JSON body, unknown enum value, type-incompatible primitives in payloads. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Malformed request body: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST,
                "Malformed request body. Check JSON syntax, enum values, and field types.");
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "Access denied: your role is not permitted to perform this action.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch: {} | path={}", msg, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        log.warn("Missing parameter: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegal(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.");
    }

    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(status, message, null));
    }
}

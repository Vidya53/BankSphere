package com.cts.notificationservice.exception;

import com.cts.notificationservice.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument: {} | path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
    }

    /** Bean-validation failures on path / query / single-arg parameters (e.g. @Min on a @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {} | path={}", detail, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, "Validation failed", detail));
    }

    /** Malformed JSON body, unknown enum value, type-incompatible primitives in payloads. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Malformed request body: {} | path={}", detail, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, "Malformed request body",
                        "The request body could not be parsed. Check JSON syntax, enum values, and field types."));
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {} | path={}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(HttpStatus.FORBIDDEN, "Access denied",
                        "Your role is not permitted to perform this action."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred", null));
    }
}

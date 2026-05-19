package com.cts.loanservice.exception;

import com.cts.loanservice.util.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Business rule violation: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex, HttpServletRequest req) {
        log.warn("Validation error: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        log.warn("Request validation failed: {} | path={}", fieldErrors, req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors));
    }

    /** Bean-validation failures on path / query / single-arg parameters (e.g. @Min on a @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, detail);
    }

    /** Malformed JSON body, unknown enum value, type-incompatible primitives in payloads. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed request body: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST,
                "Malformed request body. Check JSON syntax, enum values, and field types.");
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "Your role is not permitted to perform this action.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String detail = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        log.warn("Missing parameter: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Translates Feign call failures into a clean response with the *real*
     * downstream message instead of letting them tumble into the generic
     * {@link Exception} handler that masks everything as "An unexpected
     * error occurred." Most common case: account-service returns 422
     * "Insufficient balance" when an EMI / prepay / foreclose debit doesn't
     * fit the customer's balance — we want that message to reach the user.
     *
     *   4xx from downstream → return the same status with the same message
     *   5xx from downstream → return 502 Bad Gateway (it's not the caller's fault)
     *   No body / unparseable → fall back to a generic, status-appropriate line
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeign(FeignException ex, HttpServletRequest req) {
        HttpStatus resolved = HttpStatus.resolve(ex.status());
        if (resolved == null) resolved = HttpStatus.BAD_GATEWAY;          // ex.status() == -1 (no response)

        String downstreamMessage = extractDownstreamMessage(ex.contentUTF8());

        if (resolved.is5xxServerError()) {
            log.error("Downstream 5xx | status={} | body={} | path={}",
                    ex.status(), ex.contentUTF8(), req.getRequestURI());
            String msg = (downstreamMessage != null && !downstreamMessage.isBlank())
                    ? downstreamMessage
                    : "A downstream service is temporarily unavailable. Please try again in a moment.";
            return buildError(HttpStatus.BAD_GATEWAY, msg);
        }

        String msg = (downstreamMessage != null && !downstreamMessage.isBlank())
                ? downstreamMessage
                : "The request was rejected by a downstream service.";
        log.warn("Downstream {} | message={} | path={}", ex.status(), msg, req.getRequestURI());
        return buildError(resolved, msg);
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

    private String extractDownstreamMessage(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            if (root.hasNonNull("message")) return root.get("message").asText();
            if (root.hasNonNull("error"))   return root.get("error").asText();
        } catch (Exception ignored) {
            // Body wasn't JSON or wasn't shaped like our envelope; give up gracefully.
        }
        return null;
    }
}

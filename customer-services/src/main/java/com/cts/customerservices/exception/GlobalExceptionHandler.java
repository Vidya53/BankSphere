package com.cts.customerservices.exception;

import com.cts.customerservices.payload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 404 Not Found ───
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", request.getRequestURI());
    }

    // ─── 409 Conflict ───
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAlreadyExists(CustomerAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Duplicate customer: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER", request.getRequestURI());
    }

    @ExceptionHandler(DuplicateKycException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateKyc(DuplicateKycException ex, HttpServletRequest request) {
        log.warn("Duplicate KYC: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, "DUPLICATE_KYC", request.getRequestURI());
    }

    // ─── 422 Unprocessable Entity ───
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", request.getRequestURI());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid status transition: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATUS_TRANSITION", request.getRequestURI());
    }

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Object>> handleKycNotVerified(KycNotVerifiedException ex, HttpServletRequest request) {
        log.warn("KYC not verified: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, "KYC_NOT_VERIFIED", request.getRequestURI());
    }

    @ExceptionHandler(LoanEligibilityException.class)
    public ResponseEntity<ApiResponse<Object>> handleLoanEligibility(LoanEligibilityException ex, HttpServletRequest request) {
        log.warn("Loan eligibility failure: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, "LOAN_INELIGIBLE", request.getRequestURI());
    }

    // ─── 403 Forbidden ───
    @ExceptionHandler(CustomerNotActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotActive(CustomerNotActiveException ex, HttpServletRequest request) {
        log.warn("Customer not active: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN, "CUSTOMER_NOT_ACTIVE", request.getRequestURI());
    }

    // ─── 410 Gone ───
    @ExceptionHandler(CustomerDeletedException.class)
    public ResponseEntity<ApiResponse<Object>> handleDeleted(CustomerDeletedException ex, HttpServletRequest request) {
        log.warn("Customer deleted: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.GONE, "CUSTOMER_DELETED", request.getRequestURI());
    }

    // ─── 400 Bad Request ───
    @ExceptionHandler(BranchNotActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleBranchNotActive(BranchNotActiveException ex, HttpServletRequest request) {
        log.warn("Branch not active: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "BRANCH_NOT_ACTIVE", request.getRequestURI());
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDocument(InvalidDocumentException ex, HttpServletRequest request) {
        log.warn("Invalid document: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {} | Path: {}", errors, request.getRequestURI());
        return buildErrorResponse(errors, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {} | Path: {}", message, request.getRequestURI());
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", request.getRequestURI());
    }

    // ─── 503 Service Unavailable ───
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Service unavailable: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", request.getRequestURI());
    }

    // ─── 500 Catch-All ───
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse("An unexpected error occurred. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request.getRequestURI());
    }

    // ─── Builder ───
    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(String message, HttpStatus status, String errorCode, String path) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status("ERROR")
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, status);
    }
}

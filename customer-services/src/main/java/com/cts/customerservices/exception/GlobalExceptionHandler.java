package com.cts.customerservices.exception;

import com.cts.customerservices.payload.ApiResponse;
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

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyExists(CustomerAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("Duplicate customer: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.CONFLICT, "Customer already exists", ex.getMessage());
    }

    @ExceptionHandler(DuplicateKycException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKyc(DuplicateKycException ex, HttpServletRequest req) {
        log.warn("Duplicate KYC: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.CONFLICT, "Duplicate KYC submission", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Business rule violation: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest req) {
        log.warn("Invalid status transition: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid status transition", ex.getMessage());
    }

    @ExceptionHandler(KycNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleKycNotVerified(KycNotVerifiedException ex, HttpServletRequest req) {
        log.warn("KYC not verified: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "KYC not verified", ex.getMessage());
    }

    @ExceptionHandler(LoanEligibilityException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoanEligibility(LoanEligibilityException ex, HttpServletRequest req) {
        log.warn("Loan eligibility failure: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Loan ineligible", ex.getMessage());
    }

    @ExceptionHandler(CustomerNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotActive(CustomerNotActiveException ex, HttpServletRequest req) {
        log.warn("Customer not active: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.FORBIDDEN, "Customer not active", ex.getMessage());
    }

    @ExceptionHandler(CustomerDeletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDeleted(CustomerDeletedException ex, HttpServletRequest req) {
        log.warn("Customer deleted: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.GONE, "Customer record deleted", ex.getMessage());
    }

    @ExceptionHandler(BranchNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleBranchNotActive(BranchNotActiveException ex, HttpServletRequest req) {
        log.warn("Branch not active: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Branch not active", ex.getMessage());
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidDocument(InvalidDocumentException ex, HttpServletRequest req) {
        log.warn("Invalid document: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Invalid document", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest req) {
        log.error("Downstream service unavailable: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityValidation(ConstraintViolationException ex, HttpServletRequest req) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> {
                    String prop = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
                    String name = prop.contains(".") ? prop.substring(prop.lastIndexOf('.') + 1) : prop;
                    return name + ": " + v.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("Entity validation failed: {} | path={}", detail, req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation: {} | path={}", root, req.getRequestURI());
        // Common case: unique constraint (email/mobile already in use)
        String message = root != null && root.toLowerCase().contains("duplicate")
                ? "A record with this value already exists"
                : "Data integrity violation";
        return buildError(HttpStatus.CONFLICT, message, root);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed request body: {} | path={}", ex.getMessage(), req.getRequestURI());
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body",
                "Could not parse the request — please check field types and date formats.");
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

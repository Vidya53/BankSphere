package com.cts.transactionservice.exception;

import com.cts.transactionservice.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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

import java.util.stream.Collectors;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFound(
            TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "Transaction not found", ex.getMessage());
    }
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateTransaction(
            DuplicateTransactionException ex) {
        log.warn("Duplicate transaction attempt: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "Duplicate transaction", ex.getMessage());
    }
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransaction(
            InvalidTransactionException ex) {
        log.warn("Invalid transaction request: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid transaction", ex.getMessage());
    }
    @ExceptionHandler(IllegalTransactionStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalTransactionState(
            IllegalTransactionStateException ex) {
        log.warn("Illegal transaction state transition: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "Illegal state transition", ex.getMessage());
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
    }
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(ExternalServiceException ex) {
        log.error("External service failure: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE,
                "External service unavailable", ex.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String errorDetail = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Request body validation failed: {}", errorDetail);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", errorDetail);
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        String detail = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing request parameter: {}", detail);
        return buildError(HttpStatus.BAD_REQUEST, "Missing parameter", detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String detail = "Parameter '" + ex.getName() + "' has invalid value: '" + ex.getValue() + "'";
        log.warn("Type mismatch in request parameter: {}", detail);
        return buildError(HttpStatus.BAD_REQUEST, "Invalid parameter type", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body",
                "Request body could not be parsed. Check JSON syntax and enum values.");
    }

    /** Bean-validation failures on path / query / single-arg parameters (e.g. @NotBlank on a @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", detail);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    /** @PreAuthorize denial — caller is authenticated but their role is not permitted. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, "Access denied",
                "Your role is not permitted to perform this action.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT, "Data conflict",
                "A record with the same unique identifier already exists.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(
            OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict — concurrent modification detected: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "Concurrent modification",
                "The record was modified by another request. Please retry.");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUnexpected(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "Please contact support if the problem persists.");
    }
    private ResponseEntity<ApiResponse<Void>> buildError(HttpStatus status,
                                                          String message,
                                                          String detail) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status, message, detail));
    }
}


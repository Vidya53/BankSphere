package com.cts.accountservice.exception;

/**
 * Thrown when a controller is invoked without the API Gateway's identity
 * headers (X-User-Id, X-Role). Mapped to 401 by {@link GlobalExceptionHandler}.
 */
public class MissingGatewayHeaderException extends RuntimeException {
    public MissingGatewayHeaderException(String message) {
        super(message);
    }
}

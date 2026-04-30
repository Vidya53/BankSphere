package com.cts.accountservice.exception;

public class UnauthorizedBranchAccessException extends RuntimeException {
    public UnauthorizedBranchAccessException(String message) {
        super(message);
    }
}


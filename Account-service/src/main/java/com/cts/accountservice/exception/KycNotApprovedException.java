package com.cts.accountservice.exception;

public class KycNotApprovedException extends RuntimeException {
    public KycNotApprovedException(String message) {
        super(message);
    }
}


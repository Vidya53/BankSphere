package com.cts.branchservice.exception;

public class BranchInactiveException extends RuntimeException {
    public BranchInactiveException(String message) {
        super(message);
    }
}

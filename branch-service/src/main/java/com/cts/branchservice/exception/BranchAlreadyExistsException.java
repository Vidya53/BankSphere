package com.cts.branchservice.exception;

public class BranchAlreadyExistsException extends RuntimeException {
    public BranchAlreadyExistsException(String message) {
        super(message);
    }
}

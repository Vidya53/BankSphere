package com.cts.customerservices.exception;

public class BranchNotActiveException extends RuntimeException {

    public BranchNotActiveException(String branchCode) {
        super(String.format("Branch [%s] is either not active or does not exist. Please provide a valid branch code.", branchCode));
    }
}


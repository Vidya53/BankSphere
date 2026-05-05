package com.cts.customerservices.exception;

public class DuplicateKycException extends RuntimeException {

    public DuplicateKycException(String customerNo) {
        super(String.format("KYC documents have already been submitted for customer [%s]. Use update endpoint to modify.", customerNo));
    }
}


package com.cts.customerservices.exception;

public class KycNotVerifiedException extends RuntimeException {

    public KycNotVerifiedException(String customerNo) {
        super(String.format("KYC is not verified for customer [%s]. Please complete KYC verification first.", customerNo));
    }
}


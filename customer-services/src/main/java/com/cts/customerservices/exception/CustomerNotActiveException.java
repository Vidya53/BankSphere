package com.cts.customerservices.exception;

public class CustomerNotActiveException extends RuntimeException {

    public CustomerNotActiveException(String customerNo) {
        super(String.format("Customer [%s] account is not in ACTIVE status. Current operation requires an active account.", customerNo));
    }
}


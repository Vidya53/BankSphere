package com.cts.customerservices.exception;

public class CustomerDeletedException extends RuntimeException {

    public CustomerDeletedException(String customerNo) {
        super(String.format("Customer [%s] has been deleted and cannot be accessed.", customerNo));
    }
}


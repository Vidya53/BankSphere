package com.cts.customerservices.exception;

public class CustomerAlreadyExistsException
        extends RuntimeException {

    public CustomerAlreadyExistsException(
            String message
    ) {

        super(message);

    }

}
package com.cts.customerservices.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String serviceName) {
        super(String.format("Service [%s] is currently unavailable. Please try again later.", serviceName));
    }
}


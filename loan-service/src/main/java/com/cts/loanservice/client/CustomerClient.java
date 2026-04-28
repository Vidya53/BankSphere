package com.cts.loanservice.client;

public interface CustomerClient {
    boolean isEligible(String customerId);
}
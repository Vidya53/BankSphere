package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.CustomerClient;
import org.springframework.stereotype.Component;

@Component
public class FakeCustomerClient implements CustomerClient {

    public boolean isEligible(String customerId) {
        return true;
    }
}
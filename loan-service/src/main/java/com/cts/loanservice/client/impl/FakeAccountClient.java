package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.AccountClient;
import org.springframework.stereotype.Component;

@Component
public class FakeAccountClient implements AccountClient {

    public void credit(String accountId, Double amount) {
        System.out.println("FAKE CREDIT: " + amount);
    }

    public void debit(String accountId, Double amount) {
        System.out.println("FAKE DEBIT: " + amount);
    }
}
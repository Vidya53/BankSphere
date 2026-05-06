package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.AccountClient;

// Fake stub kept for reference only — not a Spring bean.
// Replaced by the AccountClient Feign client.
public class FakeAccountClient implements AccountClient {

    @Override
    public void credit(String accountId, Double amount) {
        System.out.println("FAKE CREDIT: " + amount);
    }

    @Override
    public void debit(String accountId, Double amount) {
        System.out.println("FAKE DEBIT: " + amount);
    }
}

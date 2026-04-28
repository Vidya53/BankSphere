package com.cts.loanservice.client;

public interface AccountClient {
    void credit(String accountId, Double amount);
    void debit(String accountId, Double amount);
}
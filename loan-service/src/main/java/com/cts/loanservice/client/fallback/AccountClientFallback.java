package com.cts.loanservice.client.fallback;

import com.cts.loanservice.client.AccountClient;
import com.cts.loanservice.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountClientFallback implements AccountClient {

    @Override
    public void credit(String accountId, Double amount) {
        log.error("account-service unavailable — cannot credit account {}", accountId);
        throw new BusinessException("Account service is unavailable. Cannot credit account: " + accountId);
    }

    @Override
    public void debit(String accountId, Double amount) {
        log.error("account-service unavailable — cannot debit account {}", accountId);
        throw new BusinessException("Account service is unavailable. Cannot debit account: " + accountId);
    }
}

package com.cts.loanservice.client.fallback;

import com.cts.loanservice.client.AccountClient;
import com.cts.loanservice.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountClientFallback implements AccountClient {

    @Override
    public com.cts.loanservice.client.dto.AccountActiveResponse isAccountActive(String accountNo) {
        // Fail-closed: if we can't reach account-service to check, refuse the
        // loan apply. Falsely approving a loan against an inactive/non-existent
        // account would later fail at disbursement anyway, and silently here
        // would mask the infra problem.
        log.error("account-service unavailable — cannot verify account active for {}", accountNo);
        com.cts.loanservice.client.dto.AccountActiveResponse r =
                new com.cts.loanservice.client.dto.AccountActiveResponse();
        r.setData(false);
        return r;
    }

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

    @Override
    public void debitWithPin(String accountId, java.util.Map<String, Object> body) {
        log.error("account-service unavailable — cannot PIN-debit account {}", accountId);
        throw new BusinessException("Account service is unavailable. Cannot debit account: " + accountId);
    }
}

package com.cts.loanservice.client;

import com.cts.loanservice.client.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "account-service",
        fallback = AccountClientFallback.class
)
public interface AccountClient {

    @org.springframework.web.bind.annotation.GetMapping("/api/v1/internal/accounts/{accountNo}/active")
    com.cts.loanservice.client.dto.AccountActiveResponse isAccountActive(
            @PathVariable("accountNo") String accountNo);

    @PostMapping("/api/v1/internal/accounts/{accountNo}/credit")
    void credit(@PathVariable("accountNo") String accountNo,
                @RequestParam("amount") Double amount);

    @PostMapping("/api/v1/internal/accounts/{accountNo}/debit")
    void debit(@PathVariable("accountNo") String accountNo,
               @RequestParam("amount") Double amount);

    /**
     * PIN-protected debit. Used for EMI payments and prepayments — the
     * customer must enter their transaction PIN, which account-service
     * verifies atomically before debiting the funds.
     *
     * Body shape: { "pin": "1234", "amount": 12345.67 }
     */
    @PostMapping("/api/v1/internal/accounts/{accountNo}/debit-with-pin")
    void debitWithPin(@PathVariable("accountNo") String accountNo,
                      @RequestBody Map<String, Object> body);
}

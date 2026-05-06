package com.cts.loanservice.client;

import com.cts.loanservice.client.fallback.AccountClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "account-service",
    fallback = AccountClientFallback.class,
    url = "${feign.client.config.account-service.url:http://localhost:8081}"
)
public interface AccountClient {

    @PostMapping("/api/v1/internal/accounts/{accountId}/credit")
    void credit(@PathVariable("accountId") String accountId,
                @RequestParam("amount") Double amount);

    @PostMapping("/api/v1/internal/accounts/{accountId}/debit")
    void debit(@PathVariable("accountId") String accountId,
               @RequestParam("amount") Double amount);
}

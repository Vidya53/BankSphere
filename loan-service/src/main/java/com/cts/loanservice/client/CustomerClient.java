package com.cts.loanservice.client;

import com.cts.loanservice.client.dto.CustomerApiResponse;
import com.cts.loanservice.client.fallback.CustomerClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-service",
        fallback = CustomerClientFallback.class
)
public interface CustomerClient {

    @GetMapping("/customers/{customerNo}")
    CustomerApiResponse getCustomerDetails(@PathVariable("customerNo") String customerNo);
}

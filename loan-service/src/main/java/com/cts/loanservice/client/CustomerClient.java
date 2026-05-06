package com.cts.loanservice.client;

import com.cts.loanservice.client.fallback.CustomerClientFallback;
import com.cts.loanservice.client.dto.CustomerApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "customer-service",
    fallback = CustomerClientFallback.class,
    url = "${feign.client.config.customer-service.url:http://localhost:8082}"
)
public interface CustomerClient {

    @GetMapping("/customers/{customerNo}")
    CustomerApiResponse getCustomerDetails(@PathVariable("customerNo") String customerNo);
}

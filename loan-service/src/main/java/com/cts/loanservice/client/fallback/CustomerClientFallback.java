package com.cts.loanservice.client.fallback;

import com.cts.loanservice.client.CustomerClient;
import com.cts.loanservice.client.dto.CustomerApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomerClientFallback implements CustomerClient {

    @Override
    public CustomerApiResponse getCustomerDetails(String customerNo) {
        log.warn("customer-service unavailable — returning empty response for customer {}", customerNo);
        return new CustomerApiResponse();
    }
}

package com.cts.loanservice.client.impl;

import com.cts.loanservice.client.CustomerClient;
import com.cts.loanservice.client.dto.CustomerApiResponse;

// Fake stub kept for reference only — not a Spring bean.
// Replaced by the CustomerClient Feign client.
public class FakeCustomerClient implements CustomerClient {

    @Override
    public CustomerApiResponse getCustomerDetails(String customerNo) {
        CustomerApiResponse response = new CustomerApiResponse();
        CustomerApiResponse.CustomerData data = new CustomerApiResponse.CustomerData();
        data.setStatus("ACTIVE");
        response.setData(data);
        return response;
    }
}

package com.cts.loanservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoanResponse {

    private Long loanId;
    private String customerId;
    private Double amount;
    private Double remainingAmount;
    private String status;
}

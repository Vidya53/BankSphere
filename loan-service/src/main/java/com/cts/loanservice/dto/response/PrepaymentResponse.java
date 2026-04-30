package com.cts.loanservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrepaymentResponse {

    private Long loanId;
    private Double prepaidAmount;
    private Double foreclosureCharge;
    private Double totalDeducted;
    private Double remainingBalance;
    private String status;
    private String message;
}


package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepaymentResponse {

    private Long loanId;
    private Double prepaidAmount;
    private Double foreclosureCharge;
    private Double totalDeducted;
    private Double remainingBalance;
    private String status;
    private String message;
}


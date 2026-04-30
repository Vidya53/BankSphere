package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoanSummaryResponse {

    private long totalLoans;
    private long activeLoans;
    private long closedLoans;
    private Double totalOutstanding;
}

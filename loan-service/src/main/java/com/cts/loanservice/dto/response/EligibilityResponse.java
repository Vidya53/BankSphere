package com.cts.loanservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EligibilityResponse {

    private boolean eligible;
    private Double maxEligibleAmount;
    private Double requestedAmount;
    private Double existingOutstanding;
    private Double monthlyIncome;
    private Double maxAllowedEmi;
    private String reason;
}


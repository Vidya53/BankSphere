package com.cts.customerservices.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LoanEligibilityResponse {
    private boolean isEligible;
    private String decision; // APPROVED or REJECTED
    private Double calculatedEmi;
    private Double maxAllowedEmi;
    private List<String> rejectionReasons;
}
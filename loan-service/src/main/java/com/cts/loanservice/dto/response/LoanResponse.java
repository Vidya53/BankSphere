package com.cts.loanservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponse {

    private Long loanId;
    private String customerId;
    private String loanType;
    private Double amount;
    private Double interestRate;
    private Integer tenureMonths;
    private Double emiAmount;
    private Double remainingAmount;
    private Integer emiPaidCount;
    private Integer emisRemaining;
    private String status;
    private LocalDate nextDueDate;
    private LocalDateTime disbursedAt;
    private String remarks;
    private LocalDateTime createdAt;
}

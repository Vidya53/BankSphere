package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    private Long paymentId;
    private Long loanId;
    private Double amountPaid;
    private Double principalComponent;
    private Double interestComponent;
    private Double penaltyAmount;
    private Double balanceAfterPayment;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private boolean late;
    private String transactionRef;
    private LocalDateTime createdAt;
}


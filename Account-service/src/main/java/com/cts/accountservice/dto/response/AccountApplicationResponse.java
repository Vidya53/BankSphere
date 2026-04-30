package com.cts.accountservice.dto.response;

import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApplicationResponse {

    private Long id;
    private String applicationRef;
    private String customerId;
    private String customerName;
    private String branchCode;
    private AccountType accountType;
    private BigDecimal initialDeposit;
    private String nomineeName;
    private String nomineeRelation;
    private String purpose;
    private ApplicationStatus status;
    private String rejectionReason;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String generatedAccountNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


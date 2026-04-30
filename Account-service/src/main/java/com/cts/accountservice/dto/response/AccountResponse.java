package com.cts.accountservice.dto.response;

import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String accountNo;
    private String customerId;
    private String customerName;
    private String branchCode;
    private String ifscCode;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal minimumBalance;
    private AccountStatus status;
    private String nomineeName;
    private String nomineeRelation;
    private Boolean isTransactional;
    private BigDecimal dailyTransferLimit;
    private BigDecimal dailyWithdrawalLimit;
    private LocalDateTime openedAt;
    private LocalDateTime updatedAt;
    private String freezeReason;
    private LocalDateTime frozenAt;
    private String closeReason;
    private LocalDateTime closedAt;
}


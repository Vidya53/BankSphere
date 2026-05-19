package com.cts.accountservice.dto.response;

import com.cts.accountservice.enums.AccountType;
import lombok.*;

import java.math.BigDecimal;

/**
 * One row per account type for a given branch — used by the admin's
 * "view branch" panel to show how many distinct customers hold each type of
 * account (e.g. SAVINGS: 142 customers across 158 accounts).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchAccountTypeBreakdown {

    private AccountType accountType;
    private long uniqueCustomers;
    private long totalAccounts;
    private BigDecimal totalBalance;
}

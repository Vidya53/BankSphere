package com.cts.accountservice.entity;

import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_acc_account_no", columnList = "accountNo", unique = true),
        @Index(name = "idx_acc_customer_id", columnList = "customerId"),
        @Index(name = "idx_acc_branch_code", columnList = "branchCode"),
        @Index(name = "idx_acc_status", columnList = "status"),
        @Index(name = "idx_acc_branch_status", columnList = "branchCode, status"),
        @Index(name = "idx_acc_type", columnList = "accountType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNo;

    @Column(nullable = false, length = 20)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 100)
    private String customerEmail;

    @Column(length = 15)
    private String customerPhone;

    @Column(nullable = false, length = 20)
    private String branchCode;

    @Column(length = 20)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(length = 100)
    private String nomineeName;

    @Column(length = 50)
    private String nomineeRelation;

    @Column(length = 15)
    private String nomineePhone;

    @Column(length = 200)
    private String nomineeAddress;

    @Column(length = 500)
    private String freezeReason;

    @Column(length = 50)
    private String frozenBy;

    private LocalDateTime frozenAt;

    @Column(length = 500)
    private String closeReason;

    @Column(length = 50)
    private String closedBy;

    private LocalDateTime closedAt;

    @Column(length = 50)
    private String approvedBy;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime openedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTransactional = true;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dailyTransferLimit = new BigDecimal("500000.00");

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dailyWithdrawalLimit = new BigDecimal("200000.00");

    // ── Transaction PIN (BCrypt-hashed). Null until the customer sets one. ──
    @Column(length = 100)
    private String transactionPin;

    private LocalDateTime pinSetAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer pinFailedAttempts = 0;

    private LocalDateTime pinLockedUntil;
}


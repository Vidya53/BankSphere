package com.cts.accountservice.entity;

import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_applications", indexes = {
        @Index(name = "idx_app_customer_id", columnList = "customerId"),
        @Index(name = "idx_app_branch_code", columnList = "branchCode"),
        @Index(name = "idx_app_status", columnList = "status"),
        @Index(name = "idx_app_branch_status", columnList = "branchCode, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String applicationRef;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(precision = 15, scale = 2)
    private BigDecimal initialDeposit;

    @Column(length = 100)
    private String nomineeName;

    @Column(length = 50)
    private String nomineeRelation;

    @Column(length = 15)
    private String nomineePhone;

    @Column(length = 200)
    private String nomineeAddress;

    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 50)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 20)
    private String generatedAccountNo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String remarks;
}


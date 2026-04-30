package com.cts.loanservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    private String customerId;
    private String accountId;

    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    private Double amount;
    private Double interestRate;
    private Integer tenureMonths;

    private Double emiAmount;
    private Double remainingAmount;
    private Integer emiPaidCount;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private LocalDate nextDueDate;
    private LocalDateTime disbursedAt;

    private String remarks;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.emiPaidCount == null) this.emiPaidCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
package com.cts.loanservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long loanId;

    private String customerId;
    private Double amount;
    private Double interestRate;
    private Integer tenureMonths;

    private Double emiAmount;
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private LocalDateTime createdAt;
}
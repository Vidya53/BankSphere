package com.cts.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transfers above the high-value threshold are queued here until a CSR reviews
 * and approves (or rejects) them. The sender's funds are NOT debited at
 * creation time — the debit/credit only happens on CSR approval, so a rejection
 * leaves balances untouched.
 */
@Entity
@Table(name = "pending_transfers", indexes = {
        @Index(name = "idx_ptr_status", columnList = "status"),
        @Index(name = "idx_ptr_branch_status", columnList = "branchCode, status"),
        @Index(name = "idx_ptr_sender", columnList = "senderAccountNo"),
        @Index(name = "idx_ptr_initiated_by", columnList = "initiatedBy")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String reference;          // e.g. PTR-20260513-9384721

    @Column(nullable = false, length = 20)
    private String senderAccountNo;

    @Column(nullable = false, length = 20)
    private String receiverAccountNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String branchCode;         // Sender's branch — CSRs see only their own

    @Column(length = 32)
    private String channel;            // UPI / NEFT / IMPS / RTGS / INTERNAL

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String initiatedBy;        // X-User-Id at initiation time

    @Column(length = 100)
    private String initiatedByName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(length = 50)
    private String reviewedBy;         // CSR's userId

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 20)
    private String generatedTransactionRef; // After approval, the txn reference

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status { PENDING_APPROVAL, APPROVED, REJECTED, CANCELLED }
}

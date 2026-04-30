package com.cts.transactionservice.model.entity;
import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_sender_account",   columnList = "sender_account_id"),
                @Index(name = "idx_receiver_account", columnList = "receiver_account_id"),
                @Index(name = "idx_status",           columnList = "status"),
                @Index(name = "idx_created_at",       columnList = "created_at"),
                @Index(name = "idx_reference_number", columnList = "reference_number", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"senderAccountId", "receiverAccountId"}) // avoid leaking sensitive data in logs
public class Transaction {
    @Id
    @UuidGenerator
    @Column(name = "transaction_id", updatable = false, nullable = false, length = 36)
    private String transactionId;
    @Column(name = "sender_account_id", length = 20)
    private String senderAccountId;
    @Column(name = "receiver_account_id", length = 20)
    private String receiverAccountId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "sender_balance_after", precision = 19, scale = 4)
    private BigDecimal senderBalanceAfter;
    @Column(name = "receiver_balance_after", precision = 19, scale = 4)
    private BigDecimal receiverBalanceAfter;
    @Column(name = "fee", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;
    @Column(name = "tax", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private TransactionChannel channel;
    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    private String referenceNumber;
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;
    @Column(name = "parent_transaction_id", length = 36)
    private String parentTransactionId;
    @Column(name = "external_reference_id", length = 100)
    private String externalReferenceId;
    @Column(name = "description", length = 255)
    private String description;
    @Column(name = "remarks", length = 500)
    private String remarks;
    @Column(name = "initiator_ip", length = 45) // 45 supports IPv6
    private String initiatorIp;
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    @Column(name = "geolocation", length = 50)
    private String geolocation;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
        if (this.fee == null)  this.fee = BigDecimal.ZERO;
        if (this.tax == null)  this.tax = BigDecimal.ZERO;
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


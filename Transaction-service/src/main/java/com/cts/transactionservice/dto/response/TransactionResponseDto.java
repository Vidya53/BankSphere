package com.cts.transactionservice.dto.response;

import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDto {
    private String transactionId;
    private String referenceNumber;
    private String idempotencyKey;
    private String parentTransactionId;
    private String externalReferenceId;
    private String senderAccountId;
    private String receiverAccountId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fee;
    private BigDecimal tax;
    private BigDecimal netAmount;
    private BigDecimal senderBalanceAfter;
    private BigDecimal receiverBalanceAfter;
    private TransactionType transactionType;
    private TransactionStatus status;
    private TransactionChannel channel;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    private String initiatedBy;
    private String message;
    private String failureReason;
}


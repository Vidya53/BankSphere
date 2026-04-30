package com.cts.transactionservice.service;

import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface TransactionService {
    TransactionResponseDto initiateTransaction(TransactionRequestDto requestDto, String initiatedBy);
    TransactionResponseDto getTransactionById(String transactionId);
    TransactionResponseDto getTransactionByReferenceNumber(String referenceNumber);
    TransactionResponseDto getTransactionByIdempotencyKey(String idempotencyKey);
    Page<TransactionResponseDto> getTransactionsByAccountId(String accountId, Pageable pageable);
    Page<TransactionResponseDto> getTransactionsByAccountIdAndDateRange(
            String accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<TransactionResponseDto> getTransactionsByStatus(TransactionStatus status, Pageable pageable);
    Page<TransactionResponseDto> getTransactionsByAccountAndType(
            String accountId, TransactionType transactionType, Pageable pageable);
    TransactionResponseDto cancelTransaction(String transactionId, String remarks);
    TransactionResponseDto reverseTransaction(String transactionId, String remarks, String initiatedBy);
    TransactionResponseDto markAsSuccess(String transactionId,
                                         BigDecimal senderBalance,
                                         BigDecimal receiverBalance);
    TransactionResponseDto markAsFailed(String transactionId, String failureReason);
    BigDecimal getTotalTransactedAmount(String accountId, LocalDateTime from, LocalDateTime to);
    long getTransactionCountSince(String accountId, LocalDateTime since);
    int timeoutStalePendingTransactions(LocalDateTime cutoffTime);
}


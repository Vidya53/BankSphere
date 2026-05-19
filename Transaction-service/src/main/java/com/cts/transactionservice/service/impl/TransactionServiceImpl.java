package com.cts.transactionservice.service.impl;

import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.exception.DuplicateTransactionException;
import com.cts.transactionservice.exception.IllegalTransactionStateException;
import com.cts.transactionservice.exception.InvalidTransactionException;
import com.cts.transactionservice.exception.TransactionNotFoundException;
import com.cts.transactionservice.mapper.TransactionMapper;
import com.cts.transactionservice.constants.TransactionConstants;
import com.cts.transactionservice.model.entity.Transaction;
import com.cts.transactionservice.util.TransactionUtils;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import com.cts.transactionservice.repository.TransactionRepository;
import com.cts.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper     transactionMapper;
    private static final BigDecimal DAILY_TRANSFER_LIMIT  = TransactionConstants.DEFAULT_DAILY_TRANSFER_LIMIT;
    private static final BigDecimal MAX_SINGLE_TXN_AMOUNT = TransactionConstants.DEFAULT_MAX_SINGLE_AMOUNT;
    private static final int        MAX_DAILY_TXN_COUNT   = TransactionConstants.DEFAULT_MAX_DAILY_TXN_COUNT;
    private static final Set<TransactionStatus> TERMINAL_STATUSES = Set.of(
            TransactionStatus.SUCCESS,
            TransactionStatus.FAILED,
            TransactionStatus.REVERSED,
            TransactionStatus.TIMED_OUT,
            TransactionStatus.CANCELLED
    );
    private static final Set<TransactionStatus> CANCELLABLE_STATUSES = Set.of(
            TransactionStatus.PENDING
    );
    private static final Set<TransactionStatus> REVERSIBLE_STATUSES = Set.of(
            TransactionStatus.SUCCESS
    );
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponseDto initiateTransaction(TransactionRequestDto requestDto,
                                                      String initiatedBy) {
        log.info("Initiating transaction | type={} | idempotencyKey={}",
                requestDto.getTransactionType(), requestDto.getIdempotencyKey());
        transactionRepository.findByIdempotencyKey(requestDto.getIdempotencyKey())
                .ifPresent(existing -> {
                    log.warn("Duplicate transaction request detected | idempotencyKey={} | existingRef={}",
                            requestDto.getIdempotencyKey(), existing.getReferenceNumber());
                    throw new DuplicateTransactionException(
                            "Transaction already processed. Reference: " + existing.getReferenceNumber());
                });
        validateTransactionRequest(requestDto, initiatedBy);
        Transaction transaction = transactionMapper.toEntity(requestDto);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setInitiatedBy(initiatedBy);

        // Service-to-service callers (e.g. account-service after a successful
        // funds movement) may pre-mark the transaction as SUCCESS / FAILED.
        // Customer-facing flow continues to use PENDING by default.
        TransactionStatus seed = requestDto.getInitialStatus() != null
                ? requestDto.getInitialStatus()
                : TransactionStatus.PENDING;
        transaction.setStatus(seed);
        if (seed == TransactionStatus.SUCCESS || seed == TransactionStatus.FAILED) {
            transaction.setCompletedAt(LocalDateTime.now());
            if (seed == TransactionStatus.SUCCESS) {
                if (requestDto.getSenderBalanceAfter()   != null) transaction.setSenderBalanceAfter(requestDto.getSenderBalanceAfter());
                if (requestDto.getReceiverBalanceAfter() != null) transaction.setReceiverBalanceAfter(requestDto.getReceiverBalanceAfter());
            }
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction persisted | transactionId={} | referenceNumber={} | status={}",
                saved.getTransactionId(), saved.getReferenceNumber(), saved.getStatus());
        return transactionMapper.toResponseDto(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(String transactionId) {
        log.debug("Fetching transaction by ID | transactionId={}", transactionId);
        Transaction transaction = findByIdOrThrow(transactionId);
        return transactionMapper.toResponseDto(transaction);
    }
    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReferenceNumber(String referenceNumber) {
        log.debug("Fetching transaction by referenceNumber={}", referenceNumber);
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found for reference number: " + referenceNumber));
        return transactionMapper.toResponseDto(transaction);
    }
    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByIdempotencyKey(String idempotencyKey) {
        log.debug("Fetching transaction by idempotencyKey={}", idempotencyKey);
        Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found for idempotency key: " + idempotencyKey));
        return transactionMapper.toResponseDto(transaction);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccountId(String accountId,
                                                                    Pageable pageable) {
        log.debug("Fetching transaction history | accountId={}", accountId);
        return transactionRepository
                .findAllByAccountId(accountId, pageable)
                .map(tx -> transactionMapper.toResponseDto(tx, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccountIdAndDateRange(String accountId,
                                                                                LocalDateTime from,
                                                                                LocalDateTime to,
                                                                                Pageable pageable) {
        validateDateRange(from, to);
        log.debug("Fetching transaction history | accountId={} | from={} | to={}", accountId, from, to);
        return transactionRepository
                .findAllByAccountIdAndDateRange(accountId, from, to, pageable)
                .map(tx -> transactionMapper.toResponseDto(tx, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByStatus(TransactionStatus status,
                                                                 Pageable pageable) {
        log.debug("Fetching transactions by status={}", status);
        return transactionRepository
                .findByStatus(status, pageable)
                .map(transactionMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccountAndType(String accountId,
                                                                         TransactionType transactionType,
                                                                         Pageable pageable) {
        log.debug("Fetching transactions | accountId={} | type={}", accountId, transactionType);
        return transactionRepository
                .findBySenderAccountIdAndTransactionType(accountId, transactionType, pageable)
                .map(tx -> transactionMapper.toResponseDto(tx, accountId));
    }
    @Override
    @Transactional
    public TransactionResponseDto cancelTransaction(String transactionId, String remarks) {
        log.info("Cancelling transaction | transactionId={}", transactionId);
        Transaction transaction = findByIdOrThrow(transactionId);
        // ── State machine guard ──────────────────────────────────────────────
        if (!CANCELLABLE_STATUSES.contains(transaction.getStatus())) {
            throw new IllegalTransactionStateException(
                    "Cannot cancel transaction in state: " + transaction.getStatus()
                    + ". Only PENDING transactions can be cancelled.");
        }
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setRemarks(remarks);
        transaction.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction cancelled | transactionId={}", saved.getTransactionId());
        return transactionMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponseDto reverseTransaction(String transactionId,
                                                      String remarks,
                                                      String initiatedBy) {
        log.info("Reversing transaction | transactionId={} | initiatedBy={}", transactionId, initiatedBy);
        Transaction original = findByIdOrThrow(transactionId);
        if (!REVERSIBLE_STATUSES.contains(original.getStatus())) {
            throw new IllegalTransactionStateException(
                    "Cannot reverse transaction in state: " + original.getStatus()
                    + ". Only SUCCESS transactions can be reversed.");
        }
        boolean alreadyReversed = transactionRepository
                .findByParentTransactionId(original.getTransactionId())
                .stream()
                .anyMatch(t -> t.getTransactionType() == TransactionType.REVERSAL
                            && t.getStatus() != TransactionStatus.FAILED
                            && t.getStatus() != TransactionStatus.CANCELLED);
        if (alreadyReversed) {
            throw new IllegalTransactionStateException(
                    "Transaction " + transactionId + " has already been reversed.");
        }
        original.setStatus(TransactionStatus.REVERSED);
        original.setRemarks("Reversed: " + remarks);
        original.setLastModifiedBy(initiatedBy);
        transactionRepository.save(original);
        Transaction reversal = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .senderAccountId(original.getReceiverAccountId())
                .receiverAccountId(original.getSenderAccountId())
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .fee(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .transactionType(TransactionType.REVERSAL)
                .status(TransactionStatus.SUCCESS)
                .channel(original.getChannel())
                .parentTransactionId(original.getTransactionId())
                .idempotencyKey(TransactionUtils.buildReversalIdempotencyKey(original.getTransactionId()))
                .description("Reversal of transaction: " + original.getReferenceNumber())
                .remarks(remarks)
                .initiatedBy(initiatedBy)
                .completedAt(LocalDateTime.now())
                .build();
        Transaction savedReversal = transactionRepository.save(reversal);
        log.info("Reversal transaction created | reversalId={} | originalId={}",
                savedReversal.getTransactionId(), transactionId);
        return transactionMapper.toResponseDto(savedReversal);
    }

    @Override
    @Transactional
    public TransactionResponseDto markAsSuccess(String transactionId,
                                                 BigDecimal senderBalance,
                                                 BigDecimal receiverBalance) {
        log.info("Marking transaction as SUCCESS | transactionId={}", transactionId);
        Transaction transaction = findByIdOrThrow(transactionId);
        assertNotInTerminalState(transaction, "mark as SUCCESS");
        if (transaction.getStatus() != TransactionStatus.PROCESSING &&
            transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalTransactionStateException(
                    "Cannot mark as SUCCESS a transaction in state: " + transaction.getStatus());
        }
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setSenderBalanceAfter(senderBalance);
        transaction.setReceiverBalanceAfter(receiverBalance);
        transaction.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction marked SUCCESS | transactionId={} | referenceNumber={}",
                saved.getTransactionId(), saved.getReferenceNumber());
        return transactionMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public TransactionResponseDto markAsFailed(String transactionId, String failureReason) {
        log.warn("Marking transaction as FAILED | transactionId={} | reason={}", transactionId, failureReason);
        Transaction transaction = findByIdOrThrow(transactionId);
        assertNotInTerminalState(transaction, "mark as FAILED");
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setRemarks(failureReason);
        transaction.setCompletedAt(LocalDateTime.now());
        Transaction saved = transactionRepository.save(transaction);
        log.warn("Transaction marked FAILED | transactionId={} | reason={}", transactionId, failureReason);
        return transactionMapper.toResponseDto(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalTransactedAmount(String accountId,
                                               LocalDateTime from,
                                               LocalDateTime to) {
        BigDecimal total = transactionRepository
                .sumSuccessfulAmountBySenderInRange(accountId, from, to);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public long getTransactionCountSince(String accountId, LocalDateTime since) {
        return transactionRepository.countBySenderAccountIdSince(accountId, since);
    }
    @Override
    @Transactional
    public int timeoutStalePendingTransactions(LocalDateTime cutoffTime) {
        int count = transactionRepository.markStalePendingAsTimedOut(cutoffTime, LocalDateTime.now());
        log.info("Stale PENDING transactions timed out | count={}", count);
        return count;
    }
    private void validateTransactionRequest(TransactionRequestDto dto, String initiatedBy) {

        // ── 1. Sender cannot be same as receiver for TRANSFER ────────────────
        if (dto.getTransactionType() == TransactionType.TRANSFER) {
            if (dto.getSenderAccountId() == null || dto.getReceiverAccountId() == null) {
                throw new InvalidTransactionException(
                        "TRANSFER requires both senderAccountId and receiverAccountId.");
            }
            if (dto.getSenderAccountId().equals(dto.getReceiverAccountId())) {
                throw new InvalidTransactionException(
                        "Sender and receiver accounts cannot be the same for a TRANSFER.");
            }
        }
        if (dto.getTransactionType() == TransactionType.WITHDRAWAL
                || dto.getTransactionType() == TransactionType.PAYMENT) {
            if (dto.getSenderAccountId() == null || dto.getSenderAccountId().isBlank()) {
                throw new InvalidTransactionException(
                        dto.getTransactionType() + " requires a senderAccountId.");
            }
        }

        if (dto.getTransactionType() == TransactionType.DEPOSIT
                || dto.getTransactionType() == TransactionType.INTEREST) {
            if (dto.getReceiverAccountId() == null || dto.getReceiverAccountId().isBlank()) {
                throw new InvalidTransactionException(
                        dto.getTransactionType() + " requires a receiverAccountId.");
            }
        }
        if (dto.getAmount().compareTo(MAX_SINGLE_TXN_AMOUNT) > 0) {
            throw new InvalidTransactionException(
                    "Transaction amount " + dto.getAmount()
                    + " exceeds single-transaction limit of " + MAX_SINGLE_TXN_AMOUNT + ".");
        }
        if (dto.getSenderAccountId() != null) {
            LocalDateTime startOfDay = TransactionUtils.startOfToday();
            LocalDateTime now        = LocalDateTime.now();
            BigDecimal dailyTotal = transactionRepository
                    .sumSuccessfulAmountBySenderInRange(dto.getSenderAccountId(), startOfDay, now);
            dailyTotal = TransactionUtils.nullSafeDecimal(dailyTotal);
            BigDecimal projectedTotal = dailyTotal.add(dto.getAmount());
            if (TransactionUtils.exceedsLimit(projectedTotal, DAILY_TRANSFER_LIMIT)) {
                throw new InvalidTransactionException(
                        "Daily transaction limit of " + DAILY_TRANSFER_LIMIT
                        + " would be exceeded. Current daily total: " + dailyTotal
                        + ". Requested: " + dto.getAmount() + ".");
            }
            long todayCount = transactionRepository
                    .countBySenderAccountIdSince(dto.getSenderAccountId(), startOfDay);
            if (todayCount >= MAX_DAILY_TXN_COUNT) {
                throw new InvalidTransactionException(
                        "Daily transaction count limit of " + MAX_DAILY_TXN_COUNT
                        + " reached for account: " + dto.getSenderAccountId() + ".");
            }
        }
        if (dto.getTransactionType() == TransactionType.REVERSAL
                || dto.getTransactionType() == TransactionType.REFUND) {
            if (dto.getParentTransactionId() == null || dto.getParentTransactionId().isBlank()) {
                throw new InvalidTransactionException(
                        dto.getTransactionType() + " requires a parentTransactionId.");
            }
            Transaction parent = transactionRepository
                    .findByTransactionId(dto.getParentTransactionId())
                    .orElseThrow(() -> new TransactionNotFoundException(
                            "Parent transaction not found: " + dto.getParentTransactionId()));
            if (parent.getStatus() != TransactionStatus.SUCCESS) {
                throw new InvalidTransactionException(
                        "Parent transaction " + dto.getParentTransactionId()
                        + " must be in SUCCESS state for " + dto.getTransactionType()
                        + ". Current state: " + parent.getStatus());
            }
        }
    }
    private Transaction findByIdOrThrow(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found: " + transactionId));
    }
    private void assertNotInTerminalState(Transaction transaction, String attemptedAction) {
        if (TERMINAL_STATUSES.contains(transaction.getStatus())) {
            throw new IllegalTransactionStateException(
                    "Cannot " + attemptedAction + " — transaction "
                    + transaction.getTransactionId() + " is already in terminal state: "
                    + transaction.getStatus());
        }
    }
    private String generateReferenceNumber() {
        String candidate;
        int attempts = 0;
        do {
            candidate = TransactionUtils.generateReferenceNumber();
            attempts++;
            if (attempts > TransactionConstants.DEFAULT_MAX_DAILY_TXN_COUNT) {
                throw new IllegalStateException(
                        "Failed to generate a unique reference number after "
                        + attempts + " attempts.");
            }
        } while (transactionRepository.existsByReferenceNumber(candidate));
        return candidate;
    }
    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new InvalidTransactionException("Date range 'from' and 'to' must not be null.");
        }
        if (from.isAfter(to)) {
            throw new InvalidTransactionException(
                    "Date range 'from' (" + from + ") must not be after 'to' (" + to + ").");
        }
    }
}


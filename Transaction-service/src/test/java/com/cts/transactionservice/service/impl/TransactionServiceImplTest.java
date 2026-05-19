package com.cts.transactionservice.service.impl;

import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.exception.DuplicateTransactionException;
import com.cts.transactionservice.exception.IllegalTransactionStateException;
import com.cts.transactionservice.exception.InvalidTransactionException;
import com.cts.transactionservice.exception.TransactionNotFoundException;
import com.cts.transactionservice.mapper.TransactionMapper;
import com.cts.transactionservice.model.entity.Transaction;
import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import com.cts.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link TransactionServiceImpl}.
 *
 * No Spring context, no database — repository and mapper are mocked.
 * Each test seeds only the stubs it needs (LENIENT strictness avoids
 * UnnecessaryStubbingException when failure branches short-circuit early).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionServiceImpl — business logic")
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks private TransactionServiceImpl service;

    @BeforeEach
    void setup() {
        // Default: idempotency key never matches; reference number never collides.
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transactionRepository.existsByReferenceNumber(anyString())).thenReturn(false);
        // The mapper's behaviour is irrelevant — we let it return null DTOs and
        // assert on the entity captured in the save() call instead.
        when(transactionMapper.toEntity(any())).thenAnswer(inv -> {
            TransactionRequestDto dto = inv.getArgument(0);
            return Transaction.builder()
                    .senderAccountId(dto.getSenderAccountId())
                    .receiverAccountId(dto.getReceiverAccountId())
                    .amount(dto.getAmount())
                    .currency(dto.getCurrency())
                    .transactionType(dto.getTransactionType())
                    .channel(dto.getChannel())
                    .idempotencyKey(dto.getIdempotencyKey())
                    .parentTransactionId(dto.getParentTransactionId())
                    .build();
        });
        when(transactionMapper.toResponseDto(any())).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            return TransactionResponseDto.builder()
                    .transactionId(tx.getTransactionId())
                    .referenceNumber(tx.getReferenceNumber())
                    .status(tx.getStatus())
                    .amount(tx.getAmount())
                    .transactionType(tx.getTransactionType())
                    .build();
        });
    }

    private TransactionRequestDto baseRequest() {
        return TransactionRequestDto.builder()
                .senderAccountId("ACC1SEND")
                .receiverAccountId("ACC2RECV")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .transactionType(TransactionType.TRANSFER)
                .channel(TransactionChannel.NET_BANKING)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  initiateTransaction
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("initiateTransaction(...)")
    class Initiate {

        @Test
        @DisplayName("happy path — saves with PENDING status by default")
        void happyPathPending() {
            TransactionRequestDto req = baseRequest();
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setTransactionId("tx-1");
                return t;
            });

            service.initiateTransaction(req, "user1");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(saved.getInitiatedBy()).isEqualTo("user1");
            assertThat(saved.getReferenceNumber()).isNotBlank();
            assertThat(saved.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("happy path — preserves seeded SUCCESS status and stamps completedAt")
        void happyPathSeededSuccess() {
            TransactionRequestDto req = baseRequest();
            req.setInitialStatus(TransactionStatus.SUCCESS);
            req.setSenderBalanceAfter(new BigDecimal("9000.00"));
            req.setReceiverBalanceAfter(new BigDecimal("2000.00"));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setTransactionId("tx-1");
                return t;
            });

            service.initiateTransaction(req, "service-account");

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(saved.getCompletedAt()).isNotNull();
            assertThat(saved.getSenderBalanceAfter()).isEqualByComparingTo("9000.00");
            assertThat(saved.getReceiverBalanceAfter()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("throws DuplicateTransactionException when idempotency key exists")
        void duplicateIdempotencyKey() {
            TransactionRequestDto req = baseRequest();
            Transaction existing = Transaction.builder()
                    .transactionId("tx-existing")
                    .referenceNumber("TXN-OLD")
                    .build();
            when(transactionRepository.findByIdempotencyKey(req.getIdempotencyKey()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .hasMessageContaining("TXN-OLD");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidTransactionException when sender = receiver on TRANSFER")
        void senderEqualsReceiver() {
            TransactionRequestDto req = baseRequest();
            req.setReceiverAccountId(req.getSenderAccountId());

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("cannot be the same");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidTransactionException when amount exceeds 5_00_000")
        void amountExceedsSingleTxnLimit() {
            TransactionRequestDto req = baseRequest();
            req.setAmount(new BigDecimal("600000.00"));

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("exceeds single-transaction limit");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidTransactionException when daily limit would be exceeded")
        void dailyLimitExceeded() {
            TransactionRequestDto req = baseRequest();
            req.setAmount(new BigDecimal("100000.00"));
            when(transactionRepository.sumSuccessfulAmountBySenderInRange(eq("ACC1SEND"), any(), any()))
                    .thenReturn(new BigDecimal("950000.00"));

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Daily transaction limit");
        }

        @Test
        @DisplayName("throws InvalidTransactionException when daily count limit exceeded")
        void dailyCountLimitExceeded() {
            TransactionRequestDto req = baseRequest();
            when(transactionRepository.sumSuccessfulAmountBySenderInRange(anyString(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countBySenderAccountIdSince(eq("ACC1SEND"), any()))
                    .thenReturn(50L);

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Daily transaction count");
        }

        @Test
        @DisplayName("REVERSAL — throws TransactionNotFoundException when parent missing")
        void reversalParentMissing() {
            TransactionRequestDto req = baseRequest();
            req.setTransactionType(TransactionType.REVERSAL);
            req.setParentTransactionId("nonexistent");
            req.setSenderAccountId(null);
            req.setReceiverAccountId(null);
            when(transactionRepository.findByTransactionId("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.initiateTransaction(req, "user1"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("Parent transaction not found");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  cancelTransaction
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancelTransaction(...)")
    class Cancel {

        @Test
        @DisplayName("happy path — PENDING → CANCELLED")
        void happyPath() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.PENDING)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cancelTransaction("tx-1", "user requested");

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
            assertThat(tx.getRemarks()).isEqualTo("user requested");
            assertThat(tx.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws IllegalTransactionStateException when not PENDING")
        void wrongState() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.SUCCESS)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.cancelTransaction("tx-1", "reason"))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  reverseTransaction
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reverseTransaction(...)")
    class Reverse {

        private Transaction successTx() {
            return Transaction.builder()
                    .transactionId("tx-orig")
                    .senderAccountId("ACC1SEND")
                    .receiverAccountId("ACC2RECV")
                    .amount(new BigDecimal("1000.00"))
                    .currency("INR")
                    .channel(TransactionChannel.NET_BANKING)
                    .transactionType(TransactionType.TRANSFER)
                    .status(TransactionStatus.SUCCESS)
                    .referenceNumber("TXN-OLD")
                    .build();
        }

        @Test
        @DisplayName("happy path — creates REVERSAL linked to parent, flips original to REVERSED")
        void happyPath() {
            Transaction original = successTx();
            when(transactionRepository.findByTransactionId("tx-orig")).thenReturn(Optional.of(original));
            when(transactionRepository.findByParentTransactionId("tx-orig")).thenReturn(List.of());
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                if (t.getTransactionId() == null) t.setTransactionId("tx-reversal");
                return t;
            });

            service.reverseTransaction("tx-orig", "customer dispute", "csr1");

            // Original was marked REVERSED
            assertThat(original.getStatus()).isEqualTo(TransactionStatus.REVERSED);

            // Capture the *second* save — the reversal entity
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
            Transaction reversal = captor.getAllValues().get(1);
            assertThat(reversal.getTransactionType()).isEqualTo(TransactionType.REVERSAL);
            assertThat(reversal.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(reversal.getParentTransactionId()).isEqualTo("tx-orig");
            assertThat(reversal.getSenderAccountId()).isEqualTo("ACC2RECV"); // swapped
            assertThat(reversal.getReceiverAccountId()).isEqualTo("ACC1SEND"); // swapped
        }

        @Test
        @DisplayName("throws IllegalTransactionStateException when original not SUCCESS")
        void notSuccess() {
            Transaction tx = successTx();
            tx.setStatus(TransactionStatus.PENDING);
            when(transactionRepository.findByTransactionId("tx-orig")).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.reverseTransaction("tx-orig", "r", "csr1"))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessageContaining("Only SUCCESS");
        }

        @Test
        @DisplayName("throws IllegalTransactionStateException when already reversed")
        void alreadyReversed() {
            Transaction tx = successTx();
            Transaction prior = Transaction.builder()
                    .transactionId("tx-reversal-old")
                    .transactionType(TransactionType.REVERSAL)
                    .status(TransactionStatus.SUCCESS)
                    .build();
            when(transactionRepository.findByTransactionId("tx-orig")).thenReturn(Optional.of(tx));
            when(transactionRepository.findByParentTransactionId("tx-orig")).thenReturn(List.of(prior));

            assertThatThrownBy(() -> service.reverseTransaction("tx-orig", "r", "csr1"))
                    .isInstanceOf(IllegalTransactionStateException.class)
                    .hasMessageContaining("already been reversed");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  markAsSuccess
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("markAsSuccess(...)")
    class MarkAsSuccess {

        @Test
        @DisplayName("happy path — PENDING → SUCCESS with balances")
        void happyPath() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.PENDING)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markAsSuccess("tx-1", new BigDecimal("8000.00"), new BigDecimal("1500.00"));

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(tx.getSenderBalanceAfter()).isEqualByComparingTo("8000.00");
            assertThat(tx.getReceiverBalanceAfter()).isEqualByComparingTo("1500.00");
            assertThat(tx.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws IllegalTransactionStateException when transaction in terminal state")
        void terminalState() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.SUCCESS)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.markAsSuccess("tx-1",
                    new BigDecimal("100.00"), new BigDecimal("100.00")))
                    .isInstanceOf(IllegalTransactionStateException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  markAsFailed
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("markAsFailed(...)")
    class MarkAsFailed {

        @Test
        @DisplayName("happy path — sets status FAILED with the reason")
        void happyPath() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.PENDING)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markAsFailed("tx-1", "INSUFFICIENT_FUNDS");

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
            assertThat(tx.getRemarks()).isEqualTo("INSUFFICIENT_FUNDS");
            assertThat(tx.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws IllegalTransactionStateException when transaction in terminal state")
        void terminalState() {
            Transaction tx = Transaction.builder()
                    .transactionId("tx-1")
                    .status(TransactionStatus.REVERSED)
                    .build();
            when(transactionRepository.findByTransactionId("tx-1")).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.markAsFailed("tx-1", "reason"))
                    .isInstanceOf(IllegalTransactionStateException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getTransactionById
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTransactionById(...)")
    class GetById {

        @Test
        @DisplayName("throws TransactionNotFoundException when transaction is missing")
        void notFound() {
            when(transactionRepository.findByTransactionId("tx-missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransactionById("tx-missing"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("tx-missing");
        }
    }
}

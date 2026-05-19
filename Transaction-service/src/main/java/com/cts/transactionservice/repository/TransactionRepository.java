package com.cts.transactionservice.repository;
import com.cts.transactionservice.model.entity.Transaction;
import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>,
        JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByParentTransactionId(String parentTransactionId);
    Optional<Transaction> findByExternalReferenceId(String externalReferenceId);
    Page<Transaction> findBySenderAccountId(String senderAccountId, Pageable pageable);
    Page<Transaction> findByReceiverAccountId(String receiverAccountId, Pageable pageable);
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.senderAccountId = :accountId
               OR t.receiverAccountId = :accountId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByAccountId(@Param("accountId") String accountId, Pageable pageable);
    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.senderAccountId = :accountId OR t.receiverAccountId = :accountId)
              AND t.createdAt BETWEEN :from AND :to
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    Page<Transaction> findBySenderAccountIdAndTransactionType(
            String senderAccountId, TransactionType transactionType, Pageable pageable);
    Page<Transaction> findByChannel(TransactionChannel channel, Pageable pageable);
    List<Transaction> findByStatusAndTransactionType(TransactionStatus status, TransactionType transactionType);
    Page<Transaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.status = 'PENDING'
              AND t.createdAt < :cutoffTime
            """)
    List<Transaction> findStalePendingTransactions(@Param("cutoffTime") LocalDateTime cutoffTime);
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.amount >= :threshold
              AND t.createdAt BETWEEN :from AND :to
            ORDER BY t.amount DESC
            """)
    Page<Transaction> findHighValueTransactions(
            @Param("threshold") BigDecimal threshold,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.senderAccountId = :accountId
              AND t.createdAt >= :since
            """)
    long countBySenderAccountIdSince(@Param("accountId") String accountId, @Param("since") LocalDateTime since);
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.senderAccountId = :accountId
              AND t.status = 'SUCCESS'
              AND t.createdAt BETWEEN :from AND :to
            """)
    BigDecimal sumSuccessfulAmountBySenderInRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
    @Query("SELECT t.status, COUNT(t) FROM Transaction t GROUP BY t.status")
    List<Object[]> countGroupByStatus();
    @Query("SELECT t.transactionType, COUNT(t), SUM(t.amount) FROM Transaction t GROUP BY t.transactionType")
    List<Object[]> volumeGroupByTransactionType();

    @Query("""
            SELECT t.channel, COUNT(t), COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.status = 'SUCCESS'
              AND t.createdAt >= :since
            GROUP BY t.channel
            """)
    List<Object[]> volumeGroupByChannelSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT FUNCTION('DATE', t.createdAt), COUNT(t), COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.status = 'SUCCESS'
              AND t.createdAt BETWEEN :from AND :to
            GROUP BY FUNCTION('DATE', t.createdAt)
            ORDER BY FUNCTION('DATE', t.createdAt)
            """)
    List<Object[]> dailySuccessVolume(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt),
                   t.transactionType, COUNT(t), COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.status = 'SUCCESS'
              AND t.createdAt >= :since
            GROUP BY FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt), t.transactionType
            ORDER BY 1, 2
            """)
    List<Object[]> monthlySuccessVolumeByType(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS'")
    BigDecimal totalSuccessfulVolume();

    long count();

    @Modifying
    @Query("""
            UPDATE Transaction t
            SET t.status = 'TIMED_OUT', t.updatedAt = :now
            WHERE t.status = 'PENDING'
              AND t.createdAt < :cutoffTime
            """)
    int markStalePendingAsTimedOut(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("now") LocalDateTime now);
    boolean existsByIdempotencyKey(String idempotencyKey);
    boolean existsByReferenceNumber(String referenceNumber);
}


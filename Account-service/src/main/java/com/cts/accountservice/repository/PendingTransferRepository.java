package com.cts.accountservice.repository;

import com.cts.accountservice.entity.PendingTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingTransferRepository extends JpaRepository<PendingTransfer, Long> {

    Optional<PendingTransfer> findByReference(String reference);

    List<PendingTransfer> findByStatusOrderByCreatedAtAsc(PendingTransfer.Status status);

    List<PendingTransfer> findByBranchCodeAndStatusOrderByCreatedAtAsc(
            String branchCode, PendingTransfer.Status status);

    long countByBranchCodeAndStatus(String branchCode, PendingTransfer.Status status);

    long countByInitiatedByAndStatus(String initiatedBy, PendingTransfer.Status status);

    /** Customer-facing view: pending transfers they initiated, newest first. */
    List<PendingTransfer> findByInitiatedByOrderByCreatedAtDesc(String initiatedBy);

    /** Sum of all PENDING_APPROVAL amounts queued by a single account today. */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM PendingTransfer p
        WHERE p.senderAccountNo = :accountNo
          AND p.status          = 'PENDING_APPROVAL'
          AND p.createdAt       >= :since
        """)
    BigDecimal sumPendingFromAccountSince(@Param("accountNo") String accountNo,
                                          @Param("since")     LocalDateTime since);
}

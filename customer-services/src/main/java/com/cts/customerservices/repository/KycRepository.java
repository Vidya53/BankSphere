package com.cts.customerservices.repository;


import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KycRepository extends JpaRepository<Kyc, Long> {

    Optional<Kyc> findByCustomerNo(String customerNo);

    boolean existsByCustomerNo(String customerNo);

    List<Kyc> findByStatus(KycStatus status);

    List<Kyc> findByStatusAndExpiryDateBefore(
            KycStatus status,
            LocalDateTime date
    );

    // ── Custom CSR queries ───────────────────────────────────────────────────

    long countByStatus(KycStatus status);

    long countByStatusIn(Collection<KycStatus> statuses);

    List<Kyc> findByStatusInOrderBySubmittedDateAsc(Collection<KycStatus> statuses);

    /**
     * Pending KYC submissions for a specific branch — joins the customer table
     * by customerNo so the CSR only sees KYCs for customers in their branch.
     * Returns oldest submissions first (so the queue prioritises by age).
     */
    @Query("""
        SELECT k FROM Kyc k, Customer c
        WHERE k.customerNo = c.customerNo
          AND c.branchCode = :branchCode
          AND k.status IN :statuses
        ORDER BY k.submittedDate ASC
        """)
    List<Kyc> findPendingByBranch(@Param("branchCode") String branchCode,
                                  @Param("statuses")  Collection<KycStatus> statuses);

    @Query("""
        SELECT COUNT(k) FROM Kyc k, Customer c
        WHERE k.customerNo = c.customerNo
          AND c.branchCode = :branchCode
          AND k.status IN :statuses
        """)
    long countPendingByBranch(@Param("branchCode") String branchCode,
                              @Param("statuses")  Collection<KycStatus> statuses);
}

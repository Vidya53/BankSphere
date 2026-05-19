package com.cts.loanservice.repository;

import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerId(String customerId);

    List<Loan> findByCustomerIdAndStatus(String customerId, LoanStatus status);

    @Query("SELECT SUM(l.remainingAmount) FROM Loan l WHERE l.customerId = :customerId AND l.status IN ('DISBURSED', 'APPROVED')")
    Double getOutstanding(@Param("customerId") String customerId);

    @Query("SELECT SUM(l.emiAmount) FROM Loan l WHERE l.customerId = :customerId AND l.status = 'DISBURSED'")
    Double getTotalActiveEmi(@Param("customerId") String customerId);

    // ── Analytics aggregations ──────────────────────────────────────────────

    @Query("SELECT l.status, COUNT(l), COALESCE(SUM(l.amount), 0) FROM Loan l GROUP BY l.status")
    List<Object[]> aggregateByStatus();

    @Query("SELECT l.loanType, COUNT(l), COALESCE(SUM(l.amount), 0) FROM Loan l GROUP BY l.loanType")
    List<Object[]> aggregateByType();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM Loan l WHERE l.status IN ('DISBURSED','APPROVED')")
    Double totalPortfolioAmount();

    @Query("SELECT COALESCE(AVG(l.amount), 0) FROM Loan l WHERE l.status IN ('DISBURSED','APPROVED')")
    Double averageTicketSize();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status IN ('DISBURSED','APPROVED')")
    long countActive();

    @Query("""
        SELECT FUNCTION('YEAR', l.disbursedAt), FUNCTION('MONTH', l.disbursedAt),
               COUNT(l), COALESCE(SUM(l.amount), 0)
        FROM Loan l
        WHERE l.disbursedAt IS NOT NULL
        GROUP BY FUNCTION('YEAR', l.disbursedAt), FUNCTION('MONTH', l.disbursedAt)
        ORDER BY 1, 2
        """)
    List<Object[]> disbursementsByMonth();

    // ── Loan-officer dashboard queries ──────────────────────────────────────

    /** Count loans created in a date range, optionally filtered by status. */
    @Query("""
        SELECT COUNT(l) FROM Loan l
        WHERE l.createdAt BETWEEN :from AND :to
          AND (:status IS NULL OR l.status = :status)
        """)
    long countCreatedBetween(@Param("from") LocalDateTime from,
                             @Param("to")   LocalDateTime to,
                             @Param("status") LoanStatus status);

    /** Sum amount of loans disbursed within a date range. */
    @Query("""
        SELECT COALESCE(SUM(l.amount), 0) FROM Loan l
        WHERE l.disbursedAt BETWEEN :from AND :to
        """)
    Double sumDisbursedBetween(@Param("from") LocalDateTime from,
                               @Param("to")   LocalDateTime to);

    /** Average amount of loans created in a window (used for ticket size MTD). */
    @Query("""
        SELECT COALESCE(AVG(l.amount), 0) FROM Loan l
        WHERE l.createdAt BETWEEN :from AND :to
        """)
    Double averageAmountBetween(@Param("from") LocalDateTime from,
                                @Param("to")   LocalDateTime to);

    /** Recent applications, newest first — used in the pipeline table. */
    @Query("SELECT l FROM Loan l ORDER BY l.createdAt DESC")
    List<Loan> findRecent(Pageable pageable);

    /** Upcoming EMIs across all active loans. */
    @Query("""
        SELECT l FROM Loan l
        WHERE l.status = 'DISBURSED'
          AND l.nextDueDate IS NOT NULL
          AND l.nextDueDate BETWEEN :from AND :to
        ORDER BY l.nextDueDate ASC
        """)
    List<Loan> findUpcomingEmis(@Param("from") LocalDate from,
                                @Param("to")   LocalDate to,
                                Pageable pageable);

    /** Overdue EMIs: disbursed loans whose nextDueDate is in the past. */
    @Query("""
        SELECT l FROM Loan l
        WHERE l.status = 'DISBURSED'
          AND l.nextDueDate IS NOT NULL
          AND l.nextDueDate < :asOf
        ORDER BY l.nextDueDate ASC
        """)
    List<Loan> findOverdueEmis(@Param("asOf") LocalDate asOf, Pageable pageable);
}
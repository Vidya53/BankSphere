package com.cts.accountservice.repository;

import com.cts.accountservice.entity.Account;
import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNo(String accountNo);

    List<Account> findByCustomerIdOrderByOpenedAtDesc(String customerId);

    List<Account> findByCustomerIdAndStatus(String customerId, AccountStatus status);

    List<Account> findByBranchCode(String branchCode);

    Page<Account> findByBranchCode(String branchCode, Pageable pageable);

    List<Account> findByBranchCodeAndStatus(String branchCode, AccountStatus status);

    Page<Account> findByBranchCodeAndStatus(String branchCode, AccountStatus status, Pageable pageable);

    List<Account> findByBranchCodeAndAccountType(String branchCode, AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.branchCode = :branchCode AND a.balance < :minBalance AND a.status = 'ACTIVE'")
    List<Account> findLowBalanceAccounts(@Param("branchCode") String branchCode, @Param("minBalance") BigDecimal minBalance);

    long countByBranchCode(String branchCode);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.branchCode = :branchCode AND a.status = :status")
    long countByBranchCodeAndStatus(@Param("branchCode") String branchCode, @Param("status") AccountStatus status);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.branchCode = :branchCode AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByBranch(@Param("branchCode") String branchCode);

    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId AND a.accountType = :type AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByCustomerIdAndType(@Param("customerId") String customerId, @Param("type") AccountType type);

    boolean existsByAccountNo(String accountNo);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.accountNo = :accountNo AND a.status = 'ACTIVE'")
    int creditAccount(@Param("accountNo") String accountNo, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount WHERE a.accountNo = :accountNo AND a.status = 'ACTIVE' AND a.balance >= :amount")
    int debitAccount(@Param("accountNo") String accountNo, @Param("amount") BigDecimal amount);

    // ── Active-only lookups (used by transaction flow) ──────────────────────

    @Query("SELECT a FROM Account a WHERE a.accountNo = :accountNo AND a.status = 'ACTIVE'")
    Optional<Account> findActiveByAccountNo(@Param("accountNo") String accountNo);

    @Query("""
        SELECT a FROM Account a
        WHERE a.accountNo = :accountNo
          AND a.customerId = :customerId
          AND a.status = 'ACTIVE'
        """)
    Optional<Account> findActiveOwnedAccount(@Param("accountNo")  String accountNo,
                                             @Param("customerId") String customerId);

    /** Atomic debit that respects the account's minimumBalance floor. */
    @Modifying
    @Query("""
        UPDATE Account a
           SET a.balance = a.balance - :amount
         WHERE a.accountNo = :accountNo
           AND a.status = 'ACTIVE'
           AND a.balance - :amount >= a.minimumBalance
        """)
    int debitRespectingMinimum(@Param("accountNo") String accountNo,
                               @Param("amount")    BigDecimal amount);

    // ── PIN attempt counters ────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Account a SET a.pinFailedAttempts = a.pinFailedAttempts + 1 WHERE a.accountNo = :accountNo")
    int incrementPinAttempts(@Param("accountNo") String accountNo);

    @Modifying
    @Query("""
        UPDATE Account a
           SET a.pinFailedAttempts = 0,
               a.pinLockedUntil    = null
         WHERE a.accountNo = :accountNo
        """)
    int resetPinAttempts(@Param("accountNo") String accountNo);

    @Modifying
    @Query("UPDATE Account a SET a.pinLockedUntil = :until WHERE a.accountNo = :accountNo")
    int lockPinUntil(@Param("accountNo") String accountNo,
                     @Param("until")     java.time.LocalDateTime until);

    // ── Branch-scoped lookups for CSR cash operations ───────────────────────

    /** Look up an active account that belongs to the staff member's branch. */
    @Query("""
        SELECT a FROM Account a
        WHERE a.accountNo  = :accountNo
          AND a.branchCode = :branchCode
          AND a.status     = 'ACTIVE'
        """)
    Optional<Account> findActiveByAccountNoAndBranch(@Param("accountNo")  String accountNo,
                                                    @Param("branchCode") String branchCode);

    /** Atomic credit that also enforces the account is ACTIVE and transactional. */
    @Modifying
    @Query("""
        UPDATE Account a
           SET a.balance = a.balance + :amount
         WHERE a.accountNo       = :accountNo
           AND a.status          = 'ACTIVE'
           AND a.isTransactional = true
        """)
    int safeCredit(@Param("accountNo") String accountNo,
                   @Param("amount")    BigDecimal amount);

    // ── Branch-scoped analytics (branch manager dashboard) ──────────────────

    @Query("""
        SELECT COUNT(a) FROM Account a
        WHERE a.branchCode = :branchCode AND a.openedAt BETWEEN :from AND :to
        """)
    long countOpenedInBranchBetween(@Param("branchCode") String branchCode,
                                    @Param("from")       java.time.LocalDateTime from,
                                    @Param("to")         java.time.LocalDateTime to);

    @Query("""
        SELECT FUNCTION('DATE', a.openedAt), COUNT(a) FROM Account a
        WHERE a.branchCode = :branchCode AND a.openedAt BETWEEN :from AND :to
        GROUP BY FUNCTION('DATE', a.openedAt)
        ORDER BY FUNCTION('DATE', a.openedAt)
        """)
    List<Object[]> dailyOpenedByBranch(@Param("branchCode") String branchCode,
                                       @Param("from")       java.time.LocalDateTime from,
                                       @Param("to")         java.time.LocalDateTime to);

    // ── Analytics aggregations ──────────────────────────────────────────────

    @Query("SELECT COUNT(a) FROM Account a")
    long countAll();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status = 'ACTIVE'")
    BigDecimal totalBalanceOfActiveAccounts();

    @Query("SELECT a.status, COUNT(a) FROM Account a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();

    @Query("""
        SELECT a.accountType, COUNT(a), COALESCE(SUM(a.balance), 0)
        FROM Account a WHERE a.status = 'ACTIVE'
        GROUP BY a.accountType
        """)
    List<Object[]> aggregateByTypeActive();

    // Admin branch view — accounts grouped by type, with distinct-customer counts
    // and total balance. Used by the "view branch" modal on the admin dashboard.
    @Query("""
        SELECT a.accountType,
               COUNT(DISTINCT a.customerId),
               COUNT(a),
               COALESCE(SUM(a.balance), 0)
        FROM Account a
        WHERE a.branchCode = :branchCode
        GROUP BY a.accountType
        """)
    List<Object[]> accountTypeBreakdownByBranch(@Param("branchCode") String branchCode);

    // Cascade-close all of a customer's accounts when the customer is soft-deleted
    // by admin or branch manager. CLOSED accounts cannot transact, which is the
    // intended downstream effect: transfers and loan disbursements stop working.
    @Modifying
    @Query("""
        UPDATE Account a
           SET a.status = com.cts.accountservice.enums.AccountStatus.CLOSED,
               a.isTransactional = false,
               a.closedAt   = CURRENT_TIMESTAMP,
               a.closeReason = :reason,
               a.closedBy   = :closedBy
         WHERE a.customerId = :customerId
           AND a.status     <> com.cts.accountservice.enums.AccountStatus.CLOSED
        """)
    int closeAllAccountsForCustomer(@Param("customerId") String customerId,
                                    @Param("reason")     String reason,
                                    @Param("closedBy")   String closedBy);
}


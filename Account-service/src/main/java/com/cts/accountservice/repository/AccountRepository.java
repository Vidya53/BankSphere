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
}


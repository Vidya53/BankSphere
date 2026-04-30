package com.cts.accountservice.repository;

import com.cts.accountservice.entity.AccountApplication;
import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountApplicationRepository extends JpaRepository<AccountApplication, Long> {

    List<AccountApplication> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<AccountApplication> findByApplicationRef(String applicationRef);

    List<AccountApplication> findByBranchCodeAndStatus(String branchCode, ApplicationStatus status);

    Page<AccountApplication> findByBranchCodeAndStatus(String branchCode, ApplicationStatus status, Pageable pageable);

    List<AccountApplication> findByBranchCode(String branchCode);

    @Query("SELECT a FROM AccountApplication a WHERE a.branchCode = :branchCode AND a.status = :status AND a.accountType = :accountType")
    List<AccountApplication> findByBranchCodeAndStatusAndAccountType(
            @Param("branchCode") String branchCode,
            @Param("status") ApplicationStatus status,
            @Param("accountType") AccountType accountType);

    @Query("SELECT a FROM AccountApplication a WHERE a.customerId = :customerId AND a.status IN :statuses")
    List<AccountApplication> findByCustomerIdAndStatusIn(
            @Param("customerId") String customerId,
            @Param("statuses") List<ApplicationStatus> statuses);

    @Query("SELECT COUNT(a) FROM AccountApplication a WHERE a.branchCode = :branchCode AND a.status = :status")
    long countByBranchCodeAndStatus(@Param("branchCode") String branchCode, @Param("status") ApplicationStatus status);

    @Query("SELECT a FROM AccountApplication a WHERE a.branchCode = :branchCode AND a.createdAt BETWEEN :from AND :to")
    List<AccountApplication> findByBranchCodeAndDateRange(
            @Param("branchCode") String branchCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    boolean existsByCustomerIdAndAccountTypeAndStatusIn(String customerId, AccountType accountType, List<ApplicationStatus> statuses);
}


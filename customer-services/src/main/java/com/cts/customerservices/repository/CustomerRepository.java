package com.cts.customerservices.repository;

import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.RiskCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNo(String customerNo);

    Optional<Customer> findByUserId(String userId);

    boolean existsByCustomerNo(String customerNo);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    List<Customer> findByStatus(CustomerStatus status);

    List<Customer> findByBranchCode(String branchCode);

    List<Customer> findByCity(String city);

    Optional<Customer> findByMobileNumber(String mobileNumber);

    Optional<Customer> findByEmail(String email);

    List<Customer> findByRiskCategory(RiskCategory riskCategory);

    List<Customer> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Customer> findByStatusAndIsDeletedFalse(
            CustomerStatus status
    );

    // ── Custom CSR queries ───────────────────────────────────────────────────

    /** Customers in a branch created at or after `since`. Powers "New today". */
    @Query("""
        SELECT COUNT(c) FROM Customer c
        WHERE c.branchCode = :branchCode
          AND c.createdAt >= :since
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
        """)
    long countNewSince(@Param("branchCode") String branchCode,
                       @Param("since")      LocalDateTime since);

    /** Recently-onboarded customers in a branch — newest first. */
    @Query("""
        SELECT c FROM Customer c
        WHERE c.branchCode = :branchCode
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
        ORDER BY c.createdAt DESC
        """)
    List<Customer> findRecentByBranch(@Param("branchCode") String branchCode,
                                      Pageable pageable);

    /** Free-text search across name, customerNo, email and mobile (case-insensitive). */
    @Query("""
        SELECT c FROM Customer c
        WHERE (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (
                LOWER(c.firstName)  LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.lastName)   LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.email)      LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.customerNo) LIKE LOWER(CONCAT('%', :q, '%'))
             OR c.mobileNumber      LIKE CONCAT('%', :q, '%')
          )
        ORDER BY c.createdAt DESC
        """)
    List<Customer> searchCustomers(@Param("q") String query, Pageable pageable);

    // ── Analytics aggregations ───────────────────────────────────────────────

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isDeleted IS NULL OR c.isDeleted = false")
    long countAllActive();

    @Query("""
        SELECT c.status, COUNT(c) FROM Customer c
        WHERE (c.isDeleted IS NULL OR c.isDeleted = false)
        GROUP BY c.status
        """)
    List<Object[]> countByStatusGrouped();

    @Query("""
        SELECT c.riskCategory, COUNT(c) FROM Customer c
        WHERE (c.isDeleted IS NULL OR c.isDeleted = false)
          AND c.riskCategory IS NOT NULL
        GROUP BY c.riskCategory
        """)
    List<Object[]> countByRiskGrouped();

    @Query("""
        SELECT c.city, COUNT(c) FROM Customer c
        WHERE (c.isDeleted IS NULL OR c.isDeleted = false)
          AND c.city IS NOT NULL
        GROUP BY c.city ORDER BY COUNT(c) DESC
        """)
    List<Object[]> countByCityGrouped(Pageable pageable);

    @Query("""
        SELECT FUNCTION('YEAR', c.createdAt),
               FUNCTION('MONTH', c.createdAt),
               COUNT(c)
        FROM Customer c
        WHERE c.createdAt >= :since
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
        GROUP BY FUNCTION('YEAR', c.createdAt), FUNCTION('MONTH', c.createdAt)
        ORDER BY 1, 2
        """)
    List<Object[]> acquisitionByMonth(@Param("since") LocalDateTime since);
}

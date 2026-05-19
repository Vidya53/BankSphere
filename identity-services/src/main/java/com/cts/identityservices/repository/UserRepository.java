package com.cts.identityservices.repository;


import com.cts.identityservices.entity.Role;
import com.cts.identityservices.entity.Status;
import com.cts.identityservices.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // ── Admin staff queries ────────────────────────────────────────────────

    /**
     * Paginated staff listing with optional filters. Any null parameter is
     * ignored so the same query supports "all staff", "by role", "by branch"
     * or any combination.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role <> com.cts.identityservices.entity.Role.CUSTOMER
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND (:branchCode IS NULL OR u.branchCode = :branchCode)
        """)
    Page<User> findStaff(@Param("role")       Role role,
                         @Param("status")     Status status,
                         @Param("branchCode") String branchCode,
                         Pageable pageable);

    /** Lightweight count by role (used in admin KPI cards). */
    long countByRole(Role role);

    long countByStatus(Status status);

    /** Lightweight list of staff for a branch (used by branch-manager dashboard). */
    @Query("""
        SELECT u FROM User u
        WHERE u.role <> com.cts.identityservices.entity.Role.CUSTOMER
          AND u.branchCode = :branchCode
        ORDER BY u.role, u.fullName
        """)
    List<User> findStaffByBranch(@Param("branchCode") String branchCode);
}
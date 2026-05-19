package com.cts.branchservice.repository;

import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCodeAndIsDeletedFalse(String branchCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsByIfscCode(String ifscCode);

    Page<Branch> findAllByIsDeletedFalse(Pageable pageable);

    Page<Branch> findAllByStatusAndIsDeletedFalse(BranchStatus status, Pageable pageable);

    Page<Branch> findAllByBranchTypeAndIsDeletedFalse(BranchType branchType, Pageable pageable);

    Page<Branch> findAllByStatusAndBranchTypeAndIsDeletedFalse(BranchStatus status, BranchType branchType, Pageable pageable);

    List<Branch> findAllByAddressStateIgnoreCaseAndIsDeletedFalse(String state);

    List<Branch> findAllByAddressCityIgnoreCaseAndIsDeletedFalse(String city);

    @Query("""
            SELECT b FROM Branch b
            WHERE b.isDeleted = false
            AND (
                LOWER(b.branchName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.branchCode) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.address.city) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.address.state) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.ifscCode) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            """)
    List<Branch> searchBranches(@Param("query") String query);

    long countByStatusAndIsDeletedFalse(BranchStatus status);

    @Query("""
            SELECT b FROM Branch b
            WHERE b.isDeleted = false
            AND (:status IS NULL OR b.status = :status)
            AND (:branchType IS NULL OR b.branchType = :branchType)
            AND (:city IS NULL OR LOWER(b.address.city) = LOWER(:city))
            AND (:state IS NULL OR LOWER(b.address.state) = LOWER(:state))
            """)
    Page<Branch> findAllWithFilters(
            @Param("status") BranchStatus status,
            @Param("branchType") BranchType branchType,
            @Param("city") String city,
            @Param("state") String state,
            Pageable pageable);
}

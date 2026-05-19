package com.cts.branchservice.repository;

import com.cts.branchservice.entity.Employee;
import com.cts.branchservice.enums.Designation;
import com.cts.branchservice.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    Page<Employee> findAllByBranch_BranchCode(String branchCode, Pageable pageable);

    Page<Employee> findAllByBranch_BranchCodeAndStatus(String branchCode, EmployeeStatus status, Pageable pageable);

    List<Employee> findAllByBranch_BranchCode(String branchCode);

    long countByBranch_BranchCode(String branchCode);

    long countByBranch_BranchCodeAndStatus(String branchCode, EmployeeStatus status);

    Optional<Employee> findByBranch_BranchCodeAndDesignation(String branchCode, Designation designation);

    @Query("""
            SELECT e.designation, COUNT(e) FROM Employee e
            WHERE e.branch.branchCode = :branchCode
            GROUP BY e.designation
            """)
    List<Object[]> countByDesignationForBranch(@Param("branchCode") String branchCode);

    @Query("SELECT MAX(CAST(SUBSTRING(e.employeeCode, 4) AS long)) FROM Employee e WHERE e.employeeCode LIKE 'EMP%'")
    Long findMaxEmployeeSequence();
}

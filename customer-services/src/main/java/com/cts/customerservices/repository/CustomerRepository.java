package com.cts.customerservices.repository;

import com.cts.customerservices.entity.Customer;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerNo(String customerNo);

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

}


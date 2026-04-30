package com.cts.loanservice.repository;

import com.cts.loanservice.entity.Loan;
import com.cts.loanservice.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerId(String customerId);

    List<Loan> findByCustomerIdAndStatus(String customerId, LoanStatus status);

    @Query("SELECT SUM(l.remainingAmount) FROM Loan l WHERE l.customerId = :customerId AND l.status IN ('DISBURSED', 'APPROVED')")
    Double getOutstanding(@Param("customerId") String customerId);

    @Query("SELECT SUM(l.emiAmount) FROM Loan l WHERE l.customerId = :customerId AND l.status = 'DISBURSED'")
    Double getTotalActiveEmi(@Param("customerId") String customerId);
}
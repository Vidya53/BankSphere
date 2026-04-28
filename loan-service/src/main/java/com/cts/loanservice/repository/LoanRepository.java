package com.cts.loanservice.repository;

import com.cts.loanservice.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    List<Loan> findByCustomerId(String customerId);

    @Query("SELECT SUM(l.remainingAmount) FROM Loan l WHERE l.customerId = :customerId")
    Double getOutstanding(@Param("customerId") String customerId);
}
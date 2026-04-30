package com.cts.loanservice.repository;

import com.cts.loanservice.entity.EmiPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmiPaymentRepository extends JpaRepository<EmiPayment, Long> {

    List<EmiPayment> findByLoanLoanIdOrderByPaidDateDesc(Long loanId);

    long countByLoanLoanId(Long loanId);
}


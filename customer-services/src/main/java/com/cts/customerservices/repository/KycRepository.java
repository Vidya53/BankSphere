package com.cts.customerservices.repository;


import com.cts.customerservices.entity.Kyc;
import com.cts.customerservices.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KycRepository extends JpaRepository<Kyc, Long> {

    Optional<Kyc> findByCustomerNo(String customerNo);

    boolean existsByCustomerNo(String customerNo);

    List<Kyc> findByStatus(KycStatus status);

    List<Kyc> findByStatusAndExpiryDateBefore(
            KycStatus status,
            LocalDateTime date
    );

}

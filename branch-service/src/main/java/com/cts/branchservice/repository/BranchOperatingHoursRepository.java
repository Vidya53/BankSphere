package com.cts.branchservice.repository;

import com.cts.branchservice.entity.BranchOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface BranchOperatingHoursRepository extends JpaRepository<BranchOperatingHours, Long> {

    List<BranchOperatingHours> findAllByBranch_BranchCode(String branchCode);

    Optional<BranchOperatingHours> findByBranch_BranchCodeAndDayOfWeek(String branchCode, DayOfWeek dayOfWeek);

    @Modifying
    @Query("DELETE FROM BranchOperatingHours h WHERE h.branch.branchCode = :branchCode")
    void deleteAllByBranchCode(@Param("branchCode") String branchCode);
}

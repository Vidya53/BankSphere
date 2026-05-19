package com.cts.auditservice.repository;

import com.cts.auditservice.entity.AuditLog;
import com.cts.auditservice.enums.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    boolean existsByEventId(String eventId);

    Page<AuditLog> findAllByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findAllByPerformedBy(String performedBy, Pageable pageable);

    // ── Aggregation queries for summary endpoint ──────────────────────────────

    @Query("SELECT a.serviceName, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :from AND :to GROUP BY a.serviceName")
    List<Object[]> countByServiceName(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :from AND :to GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', a.timestamp), COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :from AND :to GROUP BY FUNCTION('DATE', a.timestamp) " +
           "ORDER BY FUNCTION('DATE', a.timestamp)")
    List<Object[]> countByDay(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a " +
           "WHERE a.timestamp BETWEEN :from AND :to AND a.performedBy IS NOT NULL " +
           "GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> countByPerformedBy(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp BETWEEN :from AND :to AND a.status = :status")
    long countByStatusAndDateRange(
            @Param("status") AuditStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ── Compliance metric helpers ────────────────────────────────────────────

    long countByTimestampAfter(LocalDateTime timestamp);

    long countByStatusAndTimestampAfter(AuditStatus status, LocalDateTime timestamp);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.status IN :statuses
        ORDER BY a.timestamp DESC
        """)
    List<AuditLog> findRecentBreaches(@Param("statuses") List<AuditStatus> statuses, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        ORDER BY a.timestamp DESC
        """)
    List<AuditLog> findRecent(Pageable pageable);
}

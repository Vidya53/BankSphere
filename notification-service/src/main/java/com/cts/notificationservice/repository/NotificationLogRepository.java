package com.cts.notificationservice.repository;

import com.cts.notificationservice.entity.NotificationLog;
import com.cts.notificationservice.enums.NotificationStatus;
import com.cts.notificationservice.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByNotificationIdAndChannelAndStatus(
            String notificationId, NotificationType channel, NotificationStatus status);

    Optional<NotificationLog> findByNotificationIdAndChannel(
            String notificationId, NotificationType channel);

    Page<NotificationLog> findAllByUserId(String userId, Pageable pageable);

    Page<NotificationLog> findAllByStatus(NotificationStatus status, Pageable pageable);

    // Retry candidates — FAILED logs that haven't exceeded max retries
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'FAILED' " +
           "AND n.retryCount < :maxRetries " +
           "AND n.updatedAt < :before " +
           "ORDER BY n.priority ASC, n.createdAt ASC")
    List<NotificationLog> findRetryable(
            @Param("maxRetries") int maxRetries,
            @Param("before") LocalDateTime before,
            Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationLog n " +
           "WHERE n.userId = :userId AND n.createdAt >= :since")
    long countByUserIdSince(@Param("userId") String userId,
                            @Param("since") LocalDateTime since);
}

package com.cts.auditservice.entity;

import com.cts.auditservice.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_event_id",    columnList = "event_id",    unique = true),
        @Index(name = "idx_audit_service",     columnList = "service_name"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_performed_by",columnList = "performed_by"),
        @Index(name = "idx_audit_entity",      columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_timestamp",   columnList = "event_timestamp"),
        @Index(name = "idx_audit_status",      columnList = "status")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // The timestamp the event occurred in the originating service
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    // The timestamp this row was inserted — set by Hibernate, never changed
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Immutability guards ──────────────────────────────────────────────────

    @PreUpdate
    void onUpdate() {
        throw new UnsupportedOperationException(
                "Audit logs are immutable — update not allowed on event: " + eventId);
    }

    @PreRemove
    void onRemove() {
        throw new UnsupportedOperationException(
                "Audit logs are immutable — delete not allowed on event: " + eventId);
    }
}

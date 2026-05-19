package com.cts.auditservice.service.impl;

import com.cts.auditservice.dto.AuditEventMessage;
import com.cts.auditservice.dto.response.AuditLogResponse;
import com.cts.auditservice.dto.response.AuditSummaryResponse;
import com.cts.auditservice.entity.AuditLog;
import com.cts.auditservice.enums.AuditStatus;
import com.cts.auditservice.exception.AuditLogNotFoundException;
import com.cts.auditservice.repository.AuditLogRepository;
import com.cts.auditservice.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    @Override
    public void ingest(AuditEventMessage msg) {
        // Idempotency — skip duplicate events (Kafka retry safe)
        if (repository.existsByEventId(msg.eventId())) {
            log.debug("Duplicate audit event skipped: eventId={}", msg.eventId());
            return;
        }

        AuditStatus status;
        try {
            status = AuditStatus.valueOf(msg.status() != null ? msg.status().toUpperCase() : "SUCCESS");
        } catch (IllegalArgumentException e) {
            status = AuditStatus.SUCCESS;
        }

        AuditLog log = AuditLog.builder()
                .eventId(msg.eventId())
                .requestId(msg.requestId())
                .serviceName(msg.serviceName())
                .action(msg.action())
                .entityType(msg.entityType())
                .entityId(msg.entityId())
                .performedBy(msg.performedBy())
                .userRole(msg.userRole())
                .status(status)
                .branchCode(msg.branchCode())
                .ipAddress(msg.ipAddress())
                .userAgent(msg.userAgent())
                .details(msg.details())
                .errorMessage(msg.errorMessage())
                .timestamp(msg.timestamp())
                .build();

        repository.save(log);
        this.log.debug("Audit log persisted: eventId={} action={} service={}",
                msg.eventId(), msg.action(), msg.serviceName());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> queryLogs(
            String serviceName, String action, String performedBy,
            AuditStatus status, String entityType, String entityId,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Specification<AuditLog> spec = buildSpec(serviceName, action, performedBy,
                status, entityType, entityId, from, to);
        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AuditLogNotFoundException("Audit log not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByEntity(String entityType, String entityId, Pageable pageable) {
        return repository.findAllByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getByUser(String performedBy, Pageable pageable) {
        return repository.findAllByPerformedBy(performedBy, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSummaryResponse getSummary(LocalDateTime from, LocalDateTime to) {
        long total   = repository.count();
        long success = repository.countByStatusAndDateRange(AuditStatus.SUCCESS, from, to);
        long failure = repository.countByStatusAndDateRange(AuditStatus.FAILURE, from, to);
        long pending = repository.countByStatusAndDateRange(AuditStatus.PENDING, from, to);

        Map<String, Long> byService  = toMap(repository.countByServiceName(from, to));
        Map<String, Long> byAction   = toMap(repository.countByAction(from, to));
        Map<String, Long> byDay      = toDayMap(repository.countByDay(from, to));
        Map<String, Long> topUsers   = toMap(repository.countByPerformedBy(
                from, to, PageRequest.of(0, 10)));

        return AuditSummaryResponse.builder()
                .totalEvents(total)
                .successEvents(success)
                .failureEvents(failure)
                .pendingEvents(pending)
                .from(from.toLocalDate())
                .to(to.toLocalDate())
                .eventsByService(byService)
                .eventsByAction(byAction)
                .eventsByDay(byDay)
                .topPerformers(topUsers)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Specification<AuditLog> buildSpec(
            String serviceName, String action, String performedBy,
            AuditStatus status, String entityType, String entityId,
            LocalDateTime from, LocalDateTime to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (serviceName != null && !serviceName.isBlank())
                predicates.add(cb.equal(root.get("serviceName"), serviceName));
            if (action != null && !action.isBlank())
                predicates.add(cb.equal(root.get("action"), action));
            if (performedBy != null && !performedBy.isBlank())
                predicates.add(cb.equal(root.get("performedBy"), performedBy));
            if (status != null)
                predicates.add(cb.equal(root.get("status"), status));
            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null && !entityId.isBlank())
                predicates.add(cb.equal(root.get("entityId"), entityId));
            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .requestId(log.getRequestId())
                .serviceName(log.getServiceName())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .performedBy(log.getPerformedBy())
                .userRole(log.getUserRole())
                .status(log.getStatus())
                .branchCode(log.getBranchCode())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .errorMessage(log.getErrorMessage())
                .timestamp(log.getTimestamp())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) result.put(row[0].toString(), (Long) row[1]);
        }
        return result;
    }

    private Map<String, Long> toDayMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Object[] row : rows) {
            if (row[0] != null) {
                String day = row[0] instanceof LocalDate d ? d.format(fmt) : row[0].toString();
                result.put(day, (Long) row[1]);
            }
        }
        return result;
    }
}

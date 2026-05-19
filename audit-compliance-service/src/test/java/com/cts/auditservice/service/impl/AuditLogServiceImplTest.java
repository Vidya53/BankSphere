package com.cts.auditservice.service.impl;

import com.cts.auditservice.dto.AuditEventMessage;
import com.cts.auditservice.dto.response.AuditLogResponse;
import com.cts.auditservice.dto.response.AuditSummaryResponse;
import com.cts.auditservice.entity.AuditLog;
import com.cts.auditservice.enums.AuditStatus;
import com.cts.auditservice.exception.AuditLogNotFoundException;
import com.cts.auditservice.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link AuditLogServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuditLogServiceImpl — business logic")
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository repository;

    @InjectMocks private AuditLogServiceImpl service;

    private AuditEventMessage message(String status) {
        return new AuditEventMessage(
                "EV-001", "REQ-001", "account-service", "ACCOUNT_CREATED",
                "ACCOUNT", "ACC123", "USR1", "CSR", status, "BR001",
                "127.0.0.1", "JUnit/1", "details", null, LocalDateTime.now());
    }

    private AuditLog auditLog(Long id) {
        return AuditLog.builder()
                .id(id).eventId("EV-001").requestId("REQ-001")
                .serviceName("account-service").action("ACCOUNT_CREATED")
                .entityType("ACCOUNT").entityId("ACC123")
                .performedBy("USR1").userRole("CSR")
                .status(AuditStatus.SUCCESS).branchCode("BR001")
                .ipAddress("127.0.0.1").userAgent("JUnit/1")
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  ingest
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ingest(...)")
    class Ingest {

        @Test
        @DisplayName("happy path — persists a new AuditLog")
        void happyPath() {
            AuditEventMessage msg = message("SUCCESS");
            when(repository.existsByEventId("EV-001")).thenReturn(false);

            service.ingest(msg);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());
            AuditLog saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo("EV-001");
            assertThat(saved.getAction()).isEqualTo("ACCOUNT_CREATED");
            assertThat(saved.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        }

        @Test
        @DisplayName("duplicate eventId is a no-op (does not throw)")
        void duplicateIsNoop() {
            AuditEventMessage msg = message("SUCCESS");
            when(repository.existsByEventId("EV-001")).thenReturn(true);

            service.ingest(msg);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("unknown status string defaults to SUCCESS")
        void unknownStatusDefaultsSuccess() {
            AuditEventMessage msg = message("WHAT_IS_THIS");
            when(repository.existsByEventId("EV-001")).thenReturn(false);

            service.ingest(msg);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AuditStatus.SUCCESS);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  queryLogs
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("queryLogs(...)")
    class QueryLogs {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("happy path — returns mapped page")
        void happyPath() {
            Page<AuditLog> page = new PageImpl<>(List.of(auditLog(1L)));
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Page<AuditLogResponse> result = service.queryLogs(
                    "account-service", null, null, null, null, null,
                    null, null, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEventId()).isEqualTo("EV-001");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("empty result — returns empty page")
        void emptyResult() {
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<AuditLogResponse> result = service.queryLogs(
                    null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getById
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getById(...)")
    class GetById {

        @Test
        @DisplayName("happy path — returns mapped response")
        void happyPath() {
            when(repository.findById(1L)).thenReturn(Optional.of(auditLog(1L)));

            AuditLogResponse resp = service.getById(1L);

            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getEventId()).isEqualTo("EV-001");
        }

        @Test
        @DisplayName("throws AuditLogNotFoundException when missing")
        void notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(AuditLogNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getByEntity
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getByEntity(...)")
    class GetByEntity {

        @Test
        @DisplayName("happy path — returns paged audit trail for an entity")
        void happyPath() {
            Page<AuditLog> page = new PageImpl<>(List.of(auditLog(1L), auditLog(2L)));
            when(repository.findAllByEntityTypeAndEntityId(eq("ACCOUNT"), eq("ACC123"), any()))
                    .thenReturn(page);

            Page<AuditLogResponse> result = service.getByEntity("ACCOUNT", "ACC123", PageRequest.of(0, 50));

            assertThat(result.getContent()).hasSize(2);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getByUser
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getByUser(...)")
    class GetByUser {

        @Test
        @DisplayName("happy path — returns paged audit trail for a user")
        void happyPath() {
            Page<AuditLog> page = new PageImpl<>(List.of(auditLog(1L)));
            when(repository.findAllByPerformedBy(eq("USR1"), any())).thenReturn(page);

            Page<AuditLogResponse> result = service.getByUser("USR1", PageRequest.of(0, 50));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPerformedBy()).isEqualTo("USR1");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getSummary
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getSummary(...)")
    class GetSummary {

        @Test
        @DisplayName("happy path — assembles the summary with byService/byAction/byDay/topPerformers")
        void happyPath() {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now();
            when(repository.count()).thenReturn(100L);
            when(repository.countByStatusAndDateRange(eq(AuditStatus.SUCCESS), any(), any())).thenReturn(80L);
            when(repository.countByStatusAndDateRange(eq(AuditStatus.FAILURE), any(), any())).thenReturn(15L);
            when(repository.countByStatusAndDateRange(eq(AuditStatus.PENDING), any(), any())).thenReturn(5L);
            when(repository.countByServiceName(any(), any())).thenReturn(List.of(
                    new Object[]{"account-service", 50L},
                    new Object[]{"loan-service", 30L}));
            when(repository.countByAction(any(), any())).thenReturn(List.of(
                    new Object[]{"ACCOUNT_CREATED", 25L},
                    new Object[]{"LOAN_APPROVED", 10L}));
            when(repository.countByDay(any(), any())).thenReturn(List.of(
                    new Object[]{LocalDate.now().minusDays(1), 40L},
                    new Object[]{LocalDate.now(), 60L}));
            when(repository.countByPerformedBy(any(), any(), any())).thenReturn(List.of(
                    new Object[]{"USR1", 35L},
                    new Object[]{"USR2", 20L}));

            AuditSummaryResponse resp = service.getSummary(from, to);

            assertThat(resp.getTotalEvents()).isEqualTo(100);
            assertThat(resp.getSuccessEvents()).isEqualTo(80);
            assertThat(resp.getFailureEvents()).isEqualTo(15);
            assertThat(resp.getPendingEvents()).isEqualTo(5);
            assertThat(resp.getEventsByService()).containsEntry("account-service", 50L);
            assertThat(resp.getEventsByAction()).containsEntry("ACCOUNT_CREATED", 25L);
            assertThat(resp.getEventsByDay()).hasSize(2);
            assertThat(resp.getTopPerformers()).containsEntry("USR1", 35L);
        }
    }
}

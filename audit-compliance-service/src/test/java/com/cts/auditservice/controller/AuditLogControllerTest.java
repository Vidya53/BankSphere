package com.cts.auditservice.controller;

import com.cts.auditservice.dto.response.AuditLogResponse;
import com.cts.auditservice.dto.response.AuditSummaryResponse;
import com.cts.auditservice.enums.AuditStatus;
import com.cts.auditservice.exception.AuditLogNotFoundException;
import com.cts.auditservice.exception.GlobalExceptionHandler;
import com.cts.auditservice.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link AuditLogController}.
 *
 * Uses MockMvc standaloneSetup. @PreAuthorize is NOT exercised here.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogController — HTTP contract")
class AuditLogControllerTest {

    @Mock private AuditLogService auditLogService;

    private MockMvc mockMvc;
    @SuppressWarnings("unused")
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditLogController(auditLogService))
                // Spring MVC normally registers this resolver automatically; standalone
                // MockMvc doesn't, so endpoints with a `Pageable` parameter throw 500
                // unless we wire it up by hand.
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuditLogResponse response(Long id) {
        return AuditLogResponse.builder()
                .id(id).eventId("EV-" + id).serviceName("account-service")
                .action("ACCOUNT_CREATED").entityType("ACCOUNT").entityId("ACC" + id)
                .performedBy("USR1").userRole("CSR").status(AuditStatus.SUCCESS)
                .branchCode("BR001").timestamp(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs → 200 with page")
    void queryLogs() throws Exception {
        Page<AuditLogResponse> page = new PageImpl<>(List.of(response(1L)),
                PageRequest.of(0, 50), 1);
        when(auditLogService.queryLogs(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].eventId").value("EV-1"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/{id} → 200 with single log")
    void getByIdHappy() throws Exception {
        when(auditLogService.getById(7L)).thenReturn(response(7L));

        mockMvc.perform(get("/api/v1/audit/logs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/{id} → 404 when missing")
    void getByIdNotFound() throws Exception {
        when(auditLogService.getById(404L))
                .thenThrow(new AuditLogNotFoundException("Audit log not found with id: 404"));

        mockMvc.perform(get("/api/v1/audit/logs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("404")));
    }

    @Test
    @DisplayName("GET /api/v1/audit/entity/{type}/{id} → 200")
    void getByEntity() throws Exception {
        // Pass an explicit PageRequest — the no-arg PageImpl ctor uses Pageable.unpaged(),
        // and Jackson explodes trying to read `offset` from Unpaged.
        Page<AuditLogResponse> page = new PageImpl<>(List.of(response(1L)),
                PageRequest.of(0, 50), 1);
        when(auditLogService.getByEntity(eq("ACCOUNT"), eq("ACC1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/entity/ACCOUNT/ACC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].entityType").value("ACCOUNT"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/user/{user} → 200")
    void getByUser() throws Exception {
        Page<AuditLogResponse> page = new PageImpl<>(List.of(response(1L)),
                PageRequest.of(0, 50), 1);
        when(auditLogService.getByUser(eq("USR1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit/user/USR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].performedBy").value("USR1"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/summary → 200 with aggregated counts")
    void getSummary() throws Exception {
        AuditSummaryResponse summary = AuditSummaryResponse.builder()
                .totalEvents(100L).successEvents(80L).failureEvents(15L).pendingEvents(5L)
                .from(LocalDate.now().minusDays(7)).to(LocalDate.now())
                .eventsByService(Map.of("account-service", 50L))
                .eventsByAction(Map.of("ACCOUNT_CREATED", 25L))
                .eventsByDay(Map.of("2026-05-15", 60L))
                .topPerformers(Map.of("USR1", 35L))
                .build();
        when(auditLogService.getSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/audit/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEvents").value(100))
                .andExpect(jsonPath("$.data.successEvents").value(80));
    }

    @Test
    @DisplayName("GET /api/v1/audit/actions → 200 with enum names list")
    void listActions() throws Exception {
        mockMvc.perform(get("/api/v1/audit/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").exists());
    }
}

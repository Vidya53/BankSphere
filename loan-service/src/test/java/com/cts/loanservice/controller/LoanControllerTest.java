package com.cts.loanservice.controller;

import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.response.EmiScheduleResponse;
import com.cts.loanservice.dto.response.LoanResponse;
import com.cts.loanservice.exception.GlobalExceptionHandler;
import com.cts.loanservice.service.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link LoanController} using MockMvc standaloneSetup.
 *
 * Spring Security and the JWT/header filter chain are intentionally NOT loaded;
 * this test focuses on request mapping, @Valid behaviour, and JSON envelope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanController — HTTP contract")
class LoanControllerTest {

    @Mock private LoanService loanService;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        LoanController controller = new LoanController(loanService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /loans → 201 with envelope on a valid request")
    void apply() throws Exception {
        LoanResponse svcResp = LoanResponse.builder()
                .loanId(1L).customerId("CUST-ABCD1234").accountId("SAVABCDEF12345678")
                .loanType("HOME").amount(500_000.0)
                .tenureMonths(60).remainingAmount(500_000.0)
                .status("APPLIED").build();
        when(loanService.applyLoan(any())).thenReturn(svcResp);

        LoanApplyRequest req = new LoanApplyRequest(
                "CUST-ABCD1234", "SAVABCDEF12345678", "HOME", 500_000.0, 60, 80_000.0);

        mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.loanType").value("HOME"));
    }

    @Test
    @DisplayName("POST /loans → 400 when amount is negative")
    void applyValidationFails() throws Exception {
        LoanApplyRequest req = new LoanApplyRequest(
                "CUST-ABCD1234", "SAVABCDEF12345678", "HOME", -100.0, 60, 80_000.0);

        mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /loans → 400 when JSON is malformed")
    void applyMalformedJson() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loans/{id}/decision → 200")
    void decide() throws Exception {
        LoanResponse svcResp = LoanResponse.builder()
                .loanId(1L).status("APPROVED").interestRate(10.5).emiAmount(10747.0)
                .build();
        when(loanService.decideLoan(eq(1L), any())).thenReturn(svcResp);

        LoanDecisionRequest req = new LoanDecisionRequest("APPROVED", 10.5);

        mockMvc.perform(post("/loans/1/decision")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /loans/{id}/disburse → 200")
    void disburse() throws Exception {
        LoanResponse svcResp = LoanResponse.builder()
                .loanId(1L).status("DISBURSED").build();
        when(loanService.disburse(eq(1L))).thenReturn(svcResp);

        mockMvc.perform(post("/loans/1/disburse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISBURSED"));
    }

    @Test
    @DisplayName("POST /loans/{id}/pay → 200")
    void pay() throws Exception {
        LoanResponse svcResp = LoanResponse.builder()
                .loanId(1L).status("DISBURSED").remainingAmount(90_000.0).build();
        when(loanService.payEmi(eq(1L), any())).thenReturn(svcResp);

        EmiPaymentRequest req = new EmiPaymentRequest("SAVABCDEF12345678", 10_000.0, "1234");

        mockMvc.perform(post("/loans/1/pay")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISBURSED"));
    }

    @Test
    @DisplayName("GET /loans/{id} → 200")
    void getById() throws Exception {
        LoanResponse svcResp = LoanResponse.builder()
                .loanId(7L).customerId("CUST-ABCD1234").loanType("HOME")
                .amount(500_000.0).status("APPROVED").build();
        when(loanService.getLoanById(eq(7L))).thenReturn(svcResp);

        mockMvc.perform(get("/loans/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loanId").value(7))
                .andExpect(jsonPath("$.data.loanType").value("HOME"));
    }

    @Test
    @DisplayName("GET /loans/{id}/schedule → 200")
    void getSchedule() throws Exception {
        EmiScheduleResponse schedule = new EmiScheduleResponse(
                10_747.0, 12,
                List.of(EmiScheduleResponse.EmiDetail.builder()
                        .month(1).emi(10_747.0).principal(8_000.0).interest(2_747.0).balance(490_000.0).build()));
        when(loanService.getSchedule(eq(1L))).thenReturn(schedule);

        mockMvc.perform(get("/loans/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emiAmount").value(10_747.0))
                .andExpect(jsonPath("$.data.totalMonths").value(12))
                .andExpect(jsonPath("$.data.schedule[0].month").value(1));
    }
}

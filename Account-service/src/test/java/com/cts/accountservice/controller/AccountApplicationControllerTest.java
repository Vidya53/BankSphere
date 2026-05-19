package com.cts.accountservice.controller;

import com.cts.accountservice.dto.request.AccountApplicationRequest;
import com.cts.accountservice.dto.response.AccountApplicationResponse;
import com.cts.accountservice.enums.AccountType;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.exception.GlobalExceptionHandler;
import com.cts.accountservice.context.UserContext;
import com.cts.accountservice.context.UserContextExtractor;
import com.cts.accountservice.service.AccountApplicationService;
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

import java.math.BigDecimal;
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
 * Slice-style test for {@link AccountApplicationController}.
 *
 * Uses MockMvc's {@code standaloneSetup} so the test pulls in:
 *   - the controller under test (manually constructed with mocked deps)
 *   - the {@link GlobalExceptionHandler} as @ControllerAdvice
 *   - Spring's default validation / Jackson conversion
 *
 * Spring Security and the JWT/header filter chain are intentionally
 * NOT loaded — those are exercised separately. This test focuses on
 * request mapping, @Valid behaviour, and the JSON envelope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountApplicationController — HTTP contract")
class AccountApplicationControllerTest {

    @Mock private AccountApplicationService applicationService;
    @Mock private UserContextExtractor userContextExtractor;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        AccountApplicationController controller =
                new AccountApplicationController(applicationService, userContextExtractor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/account-applications → 201 with envelope on a valid request")
    void postCreatesApplication() throws Exception {
        UserContext ctx = new UserContext("USR1", "u@e.com", "CUSTOMER",
                "BR001", "Test", "u@e.com", "1234567890");
        when(userContextExtractor.extract(any())).thenReturn(ctx);

        AccountApplicationResponse svcResponse = AccountApplicationResponse.builder()
                .id(1L).applicationRef("APP123ABC")
                .customerId("USR1").branchCode("BR001")
                .accountType(AccountType.SAVINGS)
                .status(ApplicationStatus.SUBMITTED)
                .build();
        when(applicationService.applyForAccount(any(), eq(ctx))).thenReturn(svcResponse);

        AccountApplicationRequest req = AccountApplicationRequest.builder()
                .accountType(AccountType.SAVINGS)
                .branchCode("BR001")
                .initialDeposit(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Account application submitted successfully"))
                .andExpect(jsonPath("$.data.applicationRef").value("APP123ABC"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.accountType").value("SAVINGS"));
    }

    @Test
    @DisplayName("POST → 400 when accountType is missing (Jakarta validation)")
    void postFailsWhenAccountTypeMissing() throws Exception {
        AccountApplicationRequest req = AccountApplicationRequest.builder()
                .branchCode("BR001")
                .initialDeposit(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("accountType")));
    }

    @Test
    @DisplayName("POST → 400 when branchCode is blank")
    void postFailsWhenBranchCodeBlank() throws Exception {
        AccountApplicationRequest req = AccountApplicationRequest.builder()
                .accountType(AccountType.SAVINGS)
                .branchCode("")
                .initialDeposit(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("branchCode")));
    }

    @Test
    @DisplayName("POST → 400 when initialDeposit is negative")
    void postFailsWhenInitialDepositNegative() throws Exception {
        AccountApplicationRequest req = AccountApplicationRequest.builder()
                .accountType(AccountType.SAVINGS)
                .branchCode("BR001")
                .initialDeposit(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("initialDeposit")));
    }

    @Test
    @DisplayName("POST → 400 when nominee phone has invalid pattern")
    void postFailsWhenNomineePhoneInvalid() throws Exception {
        AccountApplicationRequest req = AccountApplicationRequest.builder()
                .accountType(AccountType.SAVINGS)
                .branchCode("BR001")
                .initialDeposit(new BigDecimal("1000.00"))
                .nomineePhone("abc")
                .build();

        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("nominee")));
    }

    @Test
    @DisplayName("POST → 400 when the JSON body is malformed")
    void postFailsWhenBodyIsGarbage() throws Exception {
        mockMvc.perform(post("/api/v1/account-applications")
                        .contentType(APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    @DisplayName("GET /me → 200 with the caller's application list")
    void getMyApplications() throws Exception {
        UserContext ctx = new UserContext("USR1", "u@e.com", "CUSTOMER",
                "BR001", "Test", "u@e.com", "1234567890");
        when(userContextExtractor.extract(any())).thenReturn(ctx);

        AccountApplicationResponse a = AccountApplicationResponse.builder()
                .id(1L).applicationRef("APP000001")
                .customerId("USR1").branchCode("BR001")
                .accountType(AccountType.SAVINGS)
                .status(ApplicationStatus.SUBMITTED).build();
        when(applicationService.getMyApplications(eq(ctx))).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/account-applications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].applicationRef").value("APP000001"));
    }

    @Test
    @DisplayName("GET /{id} → 200 with the single application")
    void getApplicationById() throws Exception {
        UserContext ctx = new UserContext("USR1", "u@e.com", "CUSTOMER",
                "BR001", "Test", "u@e.com", "1234567890");
        when(userContextExtractor.extract(any())).thenReturn(ctx);

        AccountApplicationResponse a = AccountApplicationResponse.builder()
                .id(7L).applicationRef("APP000007")
                .customerId("USR1").branchCode("BR001")
                .accountType(AccountType.SAVINGS)
                .status(ApplicationStatus.SUBMITTED).build();
        when(applicationService.getApplicationById(eq(7L), eq(ctx))).thenReturn(a);

        mockMvc.perform(get("/api/v1/account-applications/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.applicationRef").value("APP000007"));
    }

    @Test
    @DisplayName("GET /{id} → 400 when id is not numeric")
    void getApplicationById_typeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/account-applications/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid parameter type"));
    }
}

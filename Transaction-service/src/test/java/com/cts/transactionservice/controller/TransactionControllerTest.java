package com.cts.transactionservice.controller;

import com.cts.transactionservice.dto.request.CancelTransactionRequestDto;
import com.cts.transactionservice.dto.request.ReverseTransactionRequestDto;
import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.exception.GlobalExceptionHandler;
import com.cts.transactionservice.model.enums.TransactionChannel;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import com.cts.transactionservice.service.TransactionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link TransactionController} using MockMvc standaloneSetup.
 *
 * Spring Security and the JWT/header filter chain are intentionally NOT loaded;
 * this test focuses on request mapping, @Valid behaviour, and JSON envelope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController — HTTP contract")
class TransactionControllerTest {

    @Mock private TransactionService transactionService;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        TransactionController controller = new TransactionController(transactionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransactionRequestDto validReq() {
        return TransactionRequestDto.builder()
                .senderAccountId("ACC1SEND")
                .receiverAccountId("ACC2RECV")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .transactionType(TransactionType.TRANSFER)
                .channel(TransactionChannel.NET_BANKING)
                .idempotencyKey("idem-key-1")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/transactions → 201 with envelope on a valid request")
    void initiate() throws Exception {
        TransactionResponseDto svcResp = TransactionResponseDto.builder()
                .transactionId("tx-1")
                .referenceNumber("TXN-20260516-001")
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .transactionType(TransactionType.TRANSFER)
                .build();
        when(transactionService.initiateTransaction(any(), anyString())).thenReturn(svcResp);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(APPLICATION_JSON)
                        .header("X-Initiated-By", "user1")
                        .content(json.writeValueAsString(validReq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions → 400 on validation failure (amount = 0)")
    void initiateValidationFailure() throws Exception {
        TransactionRequestDto req = validReq();
        req.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(APPLICATION_JSON)
                        .header("X-Initiated-By", "user1")
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions → 400 on malformed JSON")
    void initiateMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(APPLICATION_JSON)
                        .header("X-Initiated-By", "user1")
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} → 200")
    void getById() throws Exception {
        TransactionResponseDto svcResp = TransactionResponseDto.builder()
                .transactionId("tx-1")
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal("1000.00"))
                .build();
        when(transactionService.getTransactionById(eq("tx-1"))).thenReturn(svcResp);

        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("tx-1"));
    }

    @Test
    @DisplayName("PATCH /api/v1/transactions/{id}/cancel → 200")
    void cancel() throws Exception {
        TransactionResponseDto svcResp = TransactionResponseDto.builder()
                .transactionId("tx-1")
                .status(TransactionStatus.CANCELLED)
                .build();
        when(transactionService.cancelTransaction(eq("tx-1"), anyString())).thenReturn(svcResp);

        CancelTransactionRequestDto req = CancelTransactionRequestDto.builder()
                .remarks("customer requested").build();

        mockMvc.perform(patch("/api/v1/transactions/tx-1/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/transactions/{id}/reverse → 200")
    void reverse() throws Exception {
        TransactionResponseDto svcResp = TransactionResponseDto.builder()
                .transactionId("tx-reversal")
                .parentTransactionId("tx-orig")
                .transactionType(TransactionType.REVERSAL)
                .status(TransactionStatus.SUCCESS)
                .build();
        when(transactionService.reverseTransaction(eq("tx-orig"), anyString(), anyString())).thenReturn(svcResp);

        ReverseTransactionRequestDto req = ReverseTransactionRequestDto.builder()
                .remarks("dispute filed").build();

        mockMvc.perform(patch("/api/v1/transactions/tx-orig/reverse")
                        .contentType(APPLICATION_JSON)
                        .header("X-Initiated-By", "csr1")
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("REVERSAL"))
                .andExpect(jsonPath("$.data.parentTransactionId").value("tx-orig"));
    }
}

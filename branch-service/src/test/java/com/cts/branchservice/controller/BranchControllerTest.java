package com.cts.branchservice.controller;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.BranchStatusRequest;
import com.cts.branchservice.dto.response.BranchResponse;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import com.cts.branchservice.exception.BranchNotFoundException;
import com.cts.branchservice.exception.GlobalExceptionHandler;
import com.cts.branchservice.service.BranchService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link BranchController}.
 *
 * Uses MockMvc's standaloneSetup. Spring Security / Eureka are NOT loaded.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BranchController — HTTP contract")
class BranchControllerTest {

    @Mock private BranchService branchService;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BranchController(branchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private BranchCreateRequest validRequest() {
        return BranchCreateRequest.builder()
                .branchCode("BR001")
                .branchName("Main Branch")
                .branchType(BranchType.URBAN)
                .address(BranchCreateRequest.AddressRequest.builder()
                        .addressLine1("123 Main")
                        .city("Hyderabad")
                        .state("TS")
                        .postalCode("500001")
                        .country("India")
                        .build())
                .contact(BranchCreateRequest.ContactRequest.builder()
                        .primaryPhone("9876543210")
                        .build())
                // hasAtm / has24x7Service are now @NotNull Boolean — must be set explicitly.
                .hasAtm(false)
                .has24x7Service(false)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/branches → 201 with envelope on a valid request")
    void postCreatesBranch() throws Exception {
        BranchResponse resp = BranchResponse.builder()
                .branchCode("BR001").branchName("Main Branch")
                .branchType(BranchType.URBAN).status(BranchStatus.ACTIVE)
                .ifscCode("BNKS0BR0010")
                .build();
        when(branchService.createBranch(any(), eq("admin"))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/branches")
                        .header("X-User-Id", "admin")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.branchCode").value("BR001"))
                .andExpect(jsonPath("$.data.ifscCode").value("BNKS0BR0010"));
    }

    @Test
    @DisplayName("POST → 400 when branchCode is blank (validation)")
    void postFailsValidation() throws Exception {
        BranchCreateRequest req = validRequest();
        req.setBranchCode(""); // violates @NotBlank

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("GET /{branchCode} → 200 with branch")
    void getBranchHappy() throws Exception {
        BranchResponse resp = BranchResponse.builder()
                .branchCode("BR001").branchName("Main").status(BranchStatus.ACTIVE).build();
        when(branchService.getBranchByCode("BR001")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/branches/BR001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("BR001"));
    }

    @Test
    @DisplayName("GET /{branchCode} → 404 when unknown")
    void getBranchNotFound() throws Exception {
        when(branchService.getBranchByCode("BR999"))
                .thenThrow(new BranchNotFoundException("Branch not found with code: BR999"));

        mockMvc.perform(get("/api/v1/branches/BR999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("BR999")));
    }

    @Test
    @DisplayName("PATCH /{branchCode}/status → 200 with success envelope")
    void patchStatusHappy() throws Exception {
        BranchStatusRequest req = BranchStatusRequest.builder()
                .status(BranchStatus.TEMPORARILY_CLOSED).reason("renovation").build();
        doNothing().when(branchService).updateBranchStatus(eq("BR001"), any(), eq("admin"));

        mockMvc.perform(patch("/api/v1/branches/BR001/status")
                        .header("X-User-Id", "admin")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TEMPORARILY_CLOSED")));
    }

    @Test
    @DisplayName("DELETE /{branchCode} → 200 on success")
    void deleteHappy() throws Exception {
        doNothing().when(branchService).deleteBranch(eq("BR001"), eq("admin"));

        mockMvc.perform(delete("/api/v1/branches/BR001")
                        .header("X-User-Id", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /search → 200 with results")
    void searchHappy() throws Exception {
        BranchResponse a = BranchResponse.builder().branchCode("BR001").branchName("Main").build();
        when(branchService.searchBranches("main")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/branches/search").param("q", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("BR001"));
    }

    @Test
    @DisplayName("POST → 400 when JSON is malformed")
    void postMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Malformed request body")));
    }
}

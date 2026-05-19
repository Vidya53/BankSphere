package com.cts.customerservices.controller;

import com.cts.customerservices.dto.CustomerRequestDTO;
import com.cts.customerservices.dto.CustomerResponseDTO;
import com.cts.customerservices.enums.CustomerStatus;
import com.cts.customerservices.enums.Gender;
import com.cts.customerservices.enums.RiskCategory;
import com.cts.customerservices.exception.GlobalExceptionHandler;
import com.cts.customerservices.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link CustomerController}.
 *
 * Uses MockMvc's {@code standaloneSetup} so the test pulls in:
 *   - the controller under test (manually constructed with mocked deps)
 *   - the {@link GlobalExceptionHandler} as @ControllerAdvice
 *   - Spring's default validation / Jackson conversion (with JavaTimeModule)
 *
 * Spring Security and the header-based pre-auth filter chain are intentionally
 * NOT loaded — those are exercised separately.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController — HTTP contract")
class CustomerControllerTest {

    @Mock private CustomerService service;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        CustomerController controller = new CustomerController(service);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(json);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    private CustomerRequestDTO validRequest() {
        return CustomerRequestDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .email("john@example.com")
                .mobileNumber("9876543210")
                .incomeAmount(50000.0)
                .branchCode("BR001")
                .addressLine1("123 Main St")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .build();
    }

    private CustomerResponseDTO sampleResponse() {
        return CustomerResponseDTO.builder()
                .customerNo("CUST-AB12CD34")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .email("john@example.com")
                .mobileNumber("9876543210")
                .branchCode("BR001")
                .addressLine1("123 Main St")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .status(CustomerStatus.REGISTERED)
                .riskCategory(RiskCategory.LOW)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("POST /customers → 201 with envelope on a valid request")
    void registerCustomer_happyPath() throws Exception {
        when(service.registerCustomer(any(), eq("USR1001"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/customers")
                        .header("X-User-Id", "USR1001")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"))
                .andExpect(jsonPath("$.data.customerNo").value("CUST-AB12CD34"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    @DisplayName("POST /customers → 400 when mobile number is invalid")
    void registerCustomer_invalidMobile() throws Exception {
        CustomerRequestDTO req = validRequest();
        req.setMobileNumber("123"); // not a valid 10-digit Indian number

        mockMvc.perform(post("/customers")
                        .header("X-User-Id", "USR1001")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("mobileNumber")));
    }

    @Test
    @DisplayName("GET /customers/me → 200 with the caller's profile")
    void getMyProfile_happyPath() throws Exception {
        when(service.getMyProfile("USR1001")).thenReturn(sampleResponse());

        mockMvc.perform(get("/customers/me").header("X-User-Id", "USR1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.customerNo").value("CUST-AB12CD34"));
    }

    @Test
    @DisplayName("GET /customers/{customerNo} → 200 with the customer profile")
    void getCustomer_happyPath() throws Exception {
        when(service.getCustomer("CUST-AB12CD34")).thenReturn(sampleResponse());

        mockMvc.perform(get("/customers/CUST-AB12CD34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerNo").value("CUST-AB12CD34"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @DisplayName("DELETE /customers/{customerNo} → 200 on soft delete")
    void deleteCustomer_happyPath() throws Exception {
        doNothing().when(service).deleteCustomer("CUST-AB12CD34");

        mockMvc.perform(delete("/customers/CUST-AB12CD34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));
    }

    @Test
    @DisplayName("PUT /customers/{customerNo}/activate → 200 on activation")
    void activateCustomer_happyPath() throws Exception {
        doNothing().when(service).activateCustomer("CUST-AB12CD34");

        mockMvc.perform(put("/customers/CUST-AB12CD34/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer account activated"));
    }

    @Test
    @DisplayName("POST /customers → 400 when the JSON body is malformed")
    void registerCustomer_malformedBody() throws Exception {
        mockMvc.perform(post("/customers")
                        .header("X-User-Id", "USR1001")
                        .contentType(APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}

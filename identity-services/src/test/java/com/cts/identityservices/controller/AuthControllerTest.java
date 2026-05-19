package com.cts.identityservices.controller;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;
import com.cts.identityservices.dto.TokenRefreshRequest;
import com.cts.identityservices.dto.response.AuthResponse;
import com.cts.identityservices.dto.response.TokenRefreshResponse;
import com.cts.identityservices.exception.GlobalExceptionHandler;
import com.cts.identityservices.service.AuthService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice-style test for {@link AuthController}.
 *
 * Uses MockMvc's {@code standaloneSetup} so the test pulls in:
 *   - the controller under test (manually constructed with a mocked service)
 *   - the {@link GlobalExceptionHandler} as @ControllerAdvice
 *   - Spring's default validation / Jackson conversion (JavaTimeModule registered for LocalDate fields)
 *
 * No Spring Security filter chain. Focus is on request mapping, @Valid behaviour,
 * and the JSON envelope.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — HTTP contract")
class AuthControllerTest {

    @Mock private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /auth/signup ──────────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/signup → 201 with envelope on a valid request")
    void signupHappy() throws Exception {
        AuthResponse svcResp = AuthResponse.builder()
                .userId(1L)
                .email("jane@example.com")
                .fullName("Jane Doe")
                .role("CUSTOMER")
                .accessToken("ACCESS")
                .refreshToken("REFRESH")
                .tokenType("Bearer")
                .accessTokenExpiresIn(900_000L)
                .build();
        when(authService.signup(any(SignupRequest.class))).thenReturn(svcResp);

        SignupRequest req = new SignupRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@example.com");
        req.setPassword("secret123");
        req.setPhoneNumber("9876543210");
        req.setDateOfBirth(LocalDate.of(1995, 4, 12));

        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"))
                .andExpect(jsonPath("$.data.accessToken").value("ACCESS"))
                .andExpect(jsonPath("$.data.refreshToken").value("REFRESH"));
    }

    @Test
    @DisplayName("POST /auth/signup → 400 when email is malformed (Jakarta validation)")
    void signupRejectsBlankEmail() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setFullName("Jane Doe");
        req.setEmail("not-an-email");        // invalid — fails @Email format
        req.setPassword("secret123");
        req.setPhoneNumber("9876543210");
        req.setDateOfBirth(LocalDate.of(1995, 4, 12));

        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("email")));
    }

    // ── POST /auth/login ───────────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/login → 200 with token pair on valid credentials")
    void loginHappy() throws Exception {
        AuthResponse svcResp = AuthResponse.builder()
                .userId(1L)
                .email("jane@example.com")
                .fullName("Jane Doe")
                .role("CUSTOMER")
                .accessToken("ACCESS")
                .refreshToken("REFRESH")
                .tokenType("Bearer")
                .accessTokenExpiresIn(900_000L)
                .build();
        when(authService.login(any(LoginRequest.class), anyString(), anyString())).thenReturn(svcResp);

        LoginRequest req = new LoginRequest();
        req.setEmail("jane@example.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("ACCESS"))
                .andExpect(jsonPath("$.data.refreshToken").value("REFRESH"));
    }

    @Test
    @DisplayName("POST /auth/login → 400 when password is missing")
    void loginRejectsMissingPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("jane@example.com");
        // password intentionally null — fails @NotBlank

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("password")));
    }

    // ── POST /auth/refresh ─────────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/refresh → 200 with a new token pair")
    void refreshHappy() throws Exception {
        TokenRefreshResponse svcResp = TokenRefreshResponse.builder()
                .accessToken("NEW_ACCESS")
                .refreshToken("NEW_REFRESH")
                .tokenType("Bearer")
                .accessTokenExpiresIn(900_000L)
                .build();
        when(authService.refresh(eq("RAW_REFRESH"), anyString(), anyString())).thenReturn(svcResp);

        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken("RAW_REFRESH");

        mockMvc.perform(post("/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("NEW_ACCESS"))
                .andExpect(jsonPath("$.data.refreshToken").value("NEW_REFRESH"));
    }

    // ── POST /auth/logout ──────────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/logout → 200 and calls service.logout once")
    void logoutHappy() throws Exception {
        doNothing().when(authService).logout(anyString());

        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken("RAW_REFRESH");

        mockMvc.perform(post("/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("RAW_REFRESH");
    }

    // ── POST /auth/logout-all ──────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/logout-all → 200 and calls service.logoutAll with X-User-Id")
    void logoutAllHappy() throws Exception {
        doNothing().when(authService).logoutAll(anyLong());

        mockMvc.perform(post("/auth/logout-all")
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All sessions terminated"));

        verify(authService).logoutAll(101L);
    }

    // ── Malformed JSON ─────────────────────────────────────────────────────
    @Test
    @DisplayName("POST /auth/signup → 400 'Malformed request body' when JSON is garbage")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }
}

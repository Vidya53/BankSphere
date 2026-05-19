package com.cts.identityservices.service.impl;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;
import com.cts.identityservices.dto.StaffSignupRequest;
import com.cts.identityservices.dto.response.AuthResponse;
import com.cts.identityservices.dto.response.TokenRefreshResponse;
import com.cts.identityservices.entity.Role;
import com.cts.identityservices.entity.Status;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.exception.InvalidCredentialsException;
import com.cts.identityservices.exception.UserAlreadyExistsException;
import com.cts.identityservices.exception.UserNotFoundException;
import com.cts.identityservices.repository.UserRepository;
import com.cts.identityservices.security.JwtService;
import com.cts.identityservices.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link AuthServiceImpl}.
 *
 * No Spring context, no database — every collaborator is mocked. Each test
 * seeds only the stubs it needs (LENIENT strictness avoids
 * UnnecessaryStubbingException on failure-path tests that short-circuit early).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl — business logic")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthServiceImpl authService;

    private SignupRequest validSignup;
    private LoginRequest validLogin;

    @BeforeEach
    void setup() {
        validSignup = new SignupRequest();
        validSignup.setFullName("Jane Doe");
        validSignup.setEmail("jane@example.com");
        validSignup.setPassword("secret123");
        validSignup.setPhoneNumber("9876543210");
        validSignup.setDateOfBirth(LocalDate.of(1995, 4, 12));

        validLogin = new LoginRequest();
        validLogin.setEmail("jane@example.com");
        validLogin.setPassword("secret123");
    }

    // ────────────────────────────────────────────────────────────────────────
    //  signup
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("signup(...)")
    class Signup {

        @Test
        @DisplayName("happy path — creates user and issues token pair")
        void happyPath() {
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(101L);
                return u;
            });
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-jwt");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(eq(101L), any(), any())).thenReturn("refresh-token");

            AuthResponse response = authService.signup(validSignup);

            assertThat(response.getUserId()).isEqualTo(101L);
            assertThat(response.getEmail()).isEqualTo("jane@example.com");
            assertThat(response.getFullName()).isEqualTo("Jane Doe");
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
            assertThat(response.getAccessToken()).isEqualTo("access-jwt");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getAccessTokenExpiresIn()).isEqualTo(900_000L);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("HASHED");
            assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
            assertThat(saved.getBranchCode()).isNull();
        }

        @Test
        @DisplayName("rejects duplicate email")
        void duplicateEmail() {
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(validSignup))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("jane@example.com");

            verify(userRepository, never()).save(any());
            verify(jwtService, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("rejects duplicate phone number")
        void duplicatePhone() {
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(true);

            assertThatThrownBy(() -> authService.signup(validSignup))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Phone");

            verify(userRepository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  login
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login(...)")
    class Login {

        private User activeUser() {
            return User.builder()
                    .id(101L)
                    .fullName("Jane Doe")
                    .email("jane@example.com")
                    .password("HASHED")
                    .phoneNumber("9876543210")
                    .role(Role.CUSTOMER)
                    .status(Status.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("happy path — updates lastLogin and issues token pair")
        void happyPath() {
            User user = activeUser();
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateAccessToken(user)).thenReturn("access-jwt");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(101L, "Mozilla", "127.0.0.1")).thenReturn("refresh-token");

            AuthResponse response = authService.login(validLogin, "Mozilla", "127.0.0.1");

            assertThat(response.getAccessToken()).isEqualTo("access-jwt");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(user.getLastLogin()).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("unknown email → UserNotFoundException")
        void unknownEmail() {
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(validLogin, "ua", "ip"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("jane@example.com");

            verify(jwtService, never()).generateAccessToken(any());
        }

        @Test
        @DisplayName("wrong password → InvalidCredentialsException")
        void wrongPassword() {
            User user = activeUser();
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(validLogin, "ua", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).createRefreshToken(anyLong(), any(), any());
        }

        @Test
        @DisplayName("BLOCKED account → InvalidCredentialsException")
        void blockedAccount() {
            User user = activeUser();
            user.setStatus(Status.BLOCKED);
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(validLogin, "ua", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("blocked");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("SUSPENDED account → InvalidCredentialsException")
        void suspendedAccount() {
            User user = activeUser();
            user.setStatus(Status.SUSPENDED);
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(validLogin, "ua", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("suspended");

            verify(userRepository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  createStaffUser
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createStaffUser(...)")
    class CreateStaffUser {

        private StaffSignupRequest validStaff(Role role, String branchCode) {
            StaffSignupRequest r = new StaffSignupRequest();
            r.setFullName("Staff Member");
            r.setEmail("staff@example.com");
            r.setPassword("supersecret");
            r.setPhoneNumber("9999999999");
            r.setDateOfBirth(LocalDate.of(1990, 1, 1));
            r.setRole(role);
            r.setBranchCode(branchCode);
            return r;
        }

        @Test
        @DisplayName("happy path — persists staff user without issuing tokens")
        void happyPath() {
            StaffSignupRequest req = validStaff(Role.CSR, "BR001");
            when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("9999999999")).thenReturn(false);
            when(passwordEncoder.encode("supersecret")).thenReturn("HASHED-STAFF");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(202L);
                return u;
            });

            AuthResponse response = authService.createStaffUser(req);

            assertThat(response.getUserId()).isEqualTo(202L);
            assertThat(response.getRole()).isEqualTo("CSR");
            assertThat(response.getBranchCode()).isEqualTo("BR001");
            // No tokens issued — admin creates accounts; the staff member logs in to get tokens
            assertThat(response.getAccessToken()).isNull();
            assertThat(response.getRefreshToken()).isNull();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.CSR);
            assertThat(captor.getValue().getStatus()).isEqualTo(Status.ACTIVE);
            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(anyLong(), any(), any());
        }

        @Test
        @DisplayName("ADMIN role is permitted even without a branchCode")
        void adminWithoutBranchAllowed() {
            StaffSignupRequest req = validStaff(Role.ADMIN, null);
            when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("9999999999")).thenReturn(false);
            when(passwordEncoder.encode("supersecret")).thenReturn("HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(203L);
                return u;
            });

            AuthResponse response = authService.createStaffUser(req);

            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getBranchCode()).isNull();
        }

        @Test
        @DisplayName("rejects CUSTOMER role")
        void rejectsCustomerRole() {
            StaffSignupRequest req = validStaff(Role.CUSTOMER, "BR001");

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CUSTOMER");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects null role")
        void rejectsNullRole() {
            StaffSignupRequest req = validStaff(null, "BR001");

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("CSR without branchCode → IllegalArgumentException")
        void csrWithoutBranchRejected() {
            StaffSignupRequest req = validStaff(Role.CSR, null);

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("BRANCH_MANAGER with blank branchCode → IllegalArgumentException")
        void branchManagerWithBlankBranchRejected() {
            StaffSignupRequest req = validStaff(Role.BRANCH_MANAGER, "   ");

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects duplicate email")
        void duplicateEmail() {
            StaffSignupRequest req = validStaff(Role.CSR, "BR001");
            when(userRepository.existsByEmail("staff@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects duplicate phone")
        void duplicatePhone() {
            StaffSignupRequest req = validStaff(Role.CSR, "BR001");
            when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
            when(userRepository.existsByPhoneNumber("9999999999")).thenReturn(true);

            assertThatThrownBy(() -> authService.createStaffUser(req))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  refresh
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("refresh(...)")
    class Refresh {

        @Test
        @DisplayName("happy path — verifies, rotates and issues a fresh token pair")
        void happyPath() {
            User user = User.builder()
                    .id(101L).fullName("Jane Doe").email("jane@example.com")
                    .password("HASHED").role(Role.CUSTOMER).status(Status.ACTIVE)
                    .build();
            when(refreshTokenService.verifyAndRotate("RAW_REFRESH")).thenReturn(101L);
            when(userRepository.findById(101L)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("new-access");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
            when(refreshTokenService.createRefreshToken(101L, "ua", "ip")).thenReturn("new-refresh");

            TokenRefreshResponse resp = authService.refresh("RAW_REFRESH", "ua", "ip");

            assertThat(resp.getAccessToken()).isEqualTo("new-access");
            assertThat(resp.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(resp.getTokenType()).isEqualTo("Bearer");
            assertThat(resp.getAccessTokenExpiresIn()).isEqualTo(900_000L);

            verify(refreshTokenService).verifyAndRotate("RAW_REFRESH");
            verify(refreshTokenService).createRefreshToken(101L, "ua", "ip");
        }

        @Test
        @DisplayName("user no longer exists → UserNotFoundException")
        void userMissing() {
            when(refreshTokenService.verifyAndRotate("RAW_REFRESH")).thenReturn(404L);
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("RAW_REFRESH", "ua", "ip"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(refreshTokenService, never()).createRefreshToken(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("account no longer active (BLOCKED) → InvalidCredentialsException")
        void blockedSinceLogin() {
            User user = User.builder()
                    .id(101L).fullName("Jane Doe").email("jane@example.com")
                    .password("HASHED").role(Role.CUSTOMER).status(Status.BLOCKED)
                    .build();
            when(refreshTokenService.verifyAndRotate("RAW_REFRESH")).thenReturn(101L);
            when(userRepository.findById(101L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.refresh("RAW_REFRESH", "ua", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("blocked");

            verify(refreshTokenService, never()).createRefreshToken(anyLong(), anyString(), anyString());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  logout / logoutAll
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("logout / logoutAll")
    class Logout {

        @Test
        @DisplayName("logout — delegates to revokeToken")
        void logoutDelegates() {
            authService.logout("RAW_REFRESH");
            verify(refreshTokenService).revokeToken("RAW_REFRESH");
        }

        @Test
        @DisplayName("logout — idempotent (revokeToken silently ignores unknown tokens)")
        void logoutIdempotent() {
            authService.logout("RAW_REFRESH");
            authService.logout("RAW_REFRESH");
            verify(refreshTokenService, org.mockito.Mockito.times(2)).revokeToken("RAW_REFRESH");
        }

        @Test
        @DisplayName("logoutAll — delegates to revokeAllForUser with the user id")
        void logoutAllDelegates() {
            authService.logoutAll(101L);
            verify(refreshTokenService).revokeAllForUser(101L);
        }
    }
}

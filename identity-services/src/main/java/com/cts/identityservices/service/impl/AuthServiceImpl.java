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
import com.cts.identityservices.service.AuthService;
import com.cts.identityservices.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .role(Role.CUSTOMER)
                .branchCode(null)
                .status(Status.ACTIVE)
                .build();

        userRepository.save(user);
        log.info("Customer registered: id={}", user.getId());

        // Signup issues an immediate token pair so the user doesn't need a separate login
        String accessToken   = jwtService.generateAccessToken(user);
        String refreshToken  = refreshTokenService.createRefreshToken(user.getId(), null, null);
        return buildResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found for email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        assertAccountActive(user);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.info("Login successful: userId={} role={}", user.getId(), user.getRole());

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), userAgent, ipAddress);
        return buildResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse createStaffUser(StaffSignupRequest request) {
        log.info("Admin creating staff user: email={} role={} branchCode={}",
                request.getEmail(), request.getRole(), request.getBranchCode());

        // ── Validation ──────────────────────────────────────────────────────
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.getRole() == Role.CUSTOMER) {
            throw new IllegalArgumentException("Use POST /auth/signup to create CUSTOMER accounts");
        }
        // Branch-required roles must carry a branchCode
        if ((request.getRole() == Role.CSR
                || request.getRole() == Role.LOAN_OFFICER
                || request.getRole() == Role.BRANCH_MANAGER)
                && (request.getBranchCode() == null || request.getBranchCode().isBlank())) {
            throw new IllegalArgumentException(
                    "Branch code is required for " + request.getRole());
        }

        // ── Pre-check duplicates so we return a clean 409 (the DataIntegrity
        //    handler is a backstop for races) ────────────────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        // ── Build and save the user ─────────────────────────────────────────
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .role(request.getRole())
                .branchCode(blankToNull(request.getBranchCode()))
                .status(Status.ACTIVE)
                .build();

        try {
            user = userRepository.save(user);
            log.info("Staff user persisted: id={} role={} branchCode={}",
                    user.getId(), user.getRole(), user.getBranchCode());
        } catch (Exception e) {
            // Surface the real cause in logs — the GlobalExceptionHandler will
            // map common cases (DataIntegrityViolation) to 409.
            log.error("Failed to save staff user (email={}, role={}): {}",
                    request.getEmail(), request.getRole(), e.getMessage(), e);
            throw e;
        }

        // ── DO NOT generate tokens for the new staff user ───────────────────
        // The admin is creating an account someone else will use. The new
        // staff member logs in themselves at POST /auth/login, which is when
        // they should receive their own token pair. Generating tokens here
        // pollutes the refresh_tokens table with rows nobody will ever use,
        // and (more importantly) was the silent failure point that surfaced
        // as 500 Internal Server Error.
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .branchCode(user.getBranchCode())
                .tokenType("Bearer")
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    @Override
    @Transactional
    public TokenRefreshResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        // verifyAndRotate revokes the presented token and returns the owner's userId
        Long userId = refreshTokenService.verifyAndRotate(rawRefreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        assertAccountActive(user);

        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(userId, userAgent, ipAddress);

        log.info("Token pair rotated: userId={}", userId);
        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtService.getAccessTokenExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
        log.debug("Single-session logout completed");
    }

    @Override
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
        log.info("All sessions terminated for userId={}", userId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void assertAccountActive(User user) {
        if (user.getStatus() == Status.BLOCKED) {
            throw new InvalidCredentialsException("Account is blocked. Please contact support.");
        }
        if (user.getStatus() == Status.SUSPENDED) {
            throw new InvalidCredentialsException("Account is temporarily suspended. Please contact support.");
        }
    }

    private AuthResponse buildResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .branchCode(user.getBranchCode())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtService.getAccessTokenExpirationMs())
                .build();
    }
}

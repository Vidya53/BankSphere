package com.cts.identityservices.controller;

import com.cts.identityservices.dto.response.ApiResponse;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Password management — authenticated user changes their own password.
 * The X-User-Id header is injected by the API gateway after JWT validation,
 * so we trust it as the user identity. Current password must verify against
 * the stored bcrypt hash.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Password · Self-service", description = "Self-serve password change for authenticated users")
public class PasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/change-password")
    @Operation(
        summary = "Change the authenticated user's password",
        description = """
                Verifies the user's current password against the stored bcrypt hash, ensures the new
                password differs from the old, then writes the new bcrypt hash. The caller is identified
                from the X-User-Id header injected by the gateway after JWT validation.

                **Allowed roles:** Any authenticated user
                **Side effects:** Updates the password hash on the user row."""
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        String userIdHeader = httpRequest.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED,
                            "Missing authentication context",
                            "X-User-Id header is required."));
        }

        long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                            "Invalid user identifier",
                            "X-User-Id must be numeric."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND,
                            "User not found",
                            "No user with id " + userId));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED,
                            "Current password is incorrect",
                            null));
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST,
                            "New password must differ from current password",
                            null));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @Getter @Setter @NoArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        @Size(max = 64, message = "Current password must not exceed 64 characters")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "New password must contain at least one letter and one digit")
        private String newPassword;
    }
}

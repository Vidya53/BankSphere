package com.cts.identityservices.controller;

import com.cts.identityservices.dto.StaffSignupRequest;
import com.cts.identityservices.dto.StaffStatusRequest;
import com.cts.identityservices.dto.response.ApiResponse;
import com.cts.identityservices.dto.response.AuthResponse;
import com.cts.identityservices.dto.response.StaffResponse;
import com.cts.identityservices.entity.Role;
import com.cts.identityservices.entity.Status;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.exception.UserNotFoundException;
import com.cts.identityservices.repository.UserRepository;
import com.cts.identityservices.service.AuthService;
import com.cts.identityservices.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin · Staff Management", description = "ADMIN-only staff user lifecycle: create, list, view, and update status")
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    // ── Create ─────────────────────────────────────────────────────────────

    @PostMapping("/staff")
    @Operation(
        summary = "Create a staff user (CSR, BRANCH_MANAGER, LOAN_OFFICER, etc.)",
        description = """
                Provisions a new internal staff account with the requested role and branch assignment.
                CUSTOMER accounts cannot be created through this endpoint — use /auth/signup instead.

                **Allowed roles:** ADMIN
                **Side effects:** Persists a new user row; issues an initial refresh token for the new staff member."""
    )
    public ResponseEntity<ApiResponse<AuthResponse>> createStaffUser(
            @Valid @RequestBody StaffSignupRequest request) {
        AuthResponse response = authService.createStaffUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Staff user created successfully"));
    }

    // ── List ───────────────────────────────────────────────────────────────

    @GetMapping("/staff")
    @Operation(
        summary = "List staff users (paginated, with optional role / status / branch filters)",
        description = """
                Returns a paginated list of internal staff (CUSTOMER role is excluded), ordered by createdAt
                desc. Page size is capped at 200 to protect the DB.

                **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> listStaff(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String branchCode,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size) {

        Page<User> result = userRepository.findStaff(
                role, status, blankToNull(branchCode),
                PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content",       result.map(StaffResponse::from).getContent());
        body.put("page",          result.getNumber());
        body.put("size",          result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages",    result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success(body, "Staff retrieved"));
    }

    @GetMapping("/staff/summary")
    @Operation(
        summary = "Staff counts grouped by role and status — used by admin KPI cards",
        description = """
                Returns total staff counts broken down by role (excluding CUSTOMER) and by status. Drives
                the headline KPI tiles on the admin dashboard.

                **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> staffSummary() {
        Map<String, Object> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            if (r == Role.CUSTOMER) continue;
            byRole.put(r.name(), userRepository.countByRole(r));
        }
        Map<String, Object> byStatus = new LinkedHashMap<>();
        for (Status s : Status.values()) byStatus.put(s.name(), userRepository.countByStatus(s));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("byRole",   byRole);
        out.put("byStatus", byStatus);
        out.put("totalStaff", byRole.values().stream().mapToLong(v -> ((Number) v).longValue()).sum());
        return ResponseEntity.ok(ApiResponse.success(out, "Staff summary retrieved"));
    }

    @GetMapping("/staff/{id}")
    @Operation(
        summary = "Get a single staff user by id",
        description = """
                Looks up a staff user by primary key. Returns 404 if the id does not exist or belongs to
                a CUSTOMER (this endpoint is staff-only).

                **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<StaffResponse>> getStaff(@PathVariable Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with id " + id));
        if (u.getRole() == Role.CUSTOMER) {
            throw new UserNotFoundException("Staff user not found: id=" + id);
        }
        return ResponseEntity.ok(ApiResponse.success(StaffResponse.from(u), "Staff retrieved"));
    }

    // ── Activate / Deactivate / Suspend ────────────────────────────────────

    @PatchMapping("/staff/{id}/status")
    @Operation(
        summary = "Update a staff user's status (ACTIVE / BLOCKED / SUSPENDED)",
        description = """
                Transitions a staff user between ACTIVE, BLOCKED, or SUSPENDED. When the new status is
                not ACTIVE, all of the user's refresh tokens are revoked so they cannot mint new access
                tokens; the current access token still works until its ~15-min expiry.

                **Allowed roles:** ADMIN
                **Side effects:** Persists the status change; revokes refresh tokens on deactivation."""
    )
    @Transactional
    public ResponseEntity<ApiResponse<StaffResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StaffStatusRequest request) {

        User u = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No user with id " + id));
        if (u.getRole() == Role.CUSTOMER) {
            throw new IllegalArgumentException("This endpoint manages staff only; use customer endpoints for customers.");
        }

        u.setStatus(request.getStatus());
        userRepository.save(u);

        // When deactivating, also revoke active refresh tokens so the user
        // can no longer obtain new access tokens. The current access token
        // expires within 15 min on its own.
        if (request.getStatus() != Status.ACTIVE) {
            try { refreshTokenService.revokeAllForUser(u.getId()); }
            catch (Exception e) { log.warn("Failed to revoke refresh tokens for user {}: {}", id, e.getMessage()); }
        }

        log.info("Staff status updated: userId={} role={} → status={}", id, u.getRole(), u.getStatus());
        return ResponseEntity.ok(ApiResponse.success(StaffResponse.from(u),
                "Staff status updated to " + u.getStatus()));
    }

    // ── Internal endpoint for branch-manager dashboard ─────────────────────

    @GetMapping("/staff/by-branch/{branchCode}")
    @Operation(
        summary = "List all staff users assigned to a branch (no pagination)",
        description = """
                Returns every staff user mapped to the given branch code. Used primarily by the
                branch-manager dashboard.

                **Allowed roles:** ADMIN"""
    )
    public ResponseEntity<ApiResponse<List<StaffResponse>>> staffByBranch(@PathVariable String branchCode) {
        List<StaffResponse> staff = userRepository.findStaffByBranch(branchCode).stream()
                .map(StaffResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(staff, "Branch staff retrieved"));
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}

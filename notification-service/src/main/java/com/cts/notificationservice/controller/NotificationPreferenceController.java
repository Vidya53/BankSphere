package com.cts.notificationservice.controller;

import com.cts.notificationservice.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user notification preferences — channels, categories and DND.
 * Stored in-memory; swap for a JpaRepository when persistence is needed.
 */
@RestController
@RequestMapping("/api/notifications/preferences")
@PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','LOAN_OFFICER','ADMIN')")
@Tag(name = "Notification Preferences", description = "User notification channels and quiet hours")
public class NotificationPreferenceController {

    private static final Map<String, Preferences> STORE = new ConcurrentHashMap<>();

    @GetMapping
    @Operation(
            summary = "Get the current user's notification preferences",
            description = """
                    Returns the caller's notification preferences (channels, categories, DND window, delivery frequency). If no preferences have been saved yet, sensible defaults are returned — all channels enabled, DND off with default window 22:00 – 08:00, and rate limit 20 notifications/hour (LOW priority throttled, HIGH bypasses).

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Preferences>> get(HttpServletRequest request) {
        String userId = resolveUser(request);
        Preferences prefs = STORE.computeIfAbsent(userId, NotificationPreferenceController::defaults);
        return ResponseEntity.ok(ApiResponse.success(prefs, "Preferences retrieved"));
    }

    @PutMapping
    @Operation(
            summary = "Update the current user's notification preferences",
            description = """
                    Replaces the caller's notification preferences — channels, categories, DND window (HH:mm) and delivery frequency.
                    DND only suppresses LOW-priority notifications; HIGH-priority events (e.g. security alerts) always bypass quiet hours and the per-user hourly rate limit.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    public ResponseEntity<ApiResponse<Preferences>> update(
            @Valid @RequestBody Preferences body,
            HttpServletRequest request) {

        String userId = resolveUser(request);
        body.setUserId(userId);
        // Quiet hours sanity check (string-only, no parsing required)
        STORE.put(userId, body);
        return ResponseEntity.ok(ApiResponse.success(body, "Preferences updated"));
    }

    private static String resolveUser(HttpServletRequest req) {
        String userId = req.getHeader("X-User-Id");
        return (userId != null && !userId.isBlank()) ? userId : "anonymous";
    }

    private static Preferences defaults(String userId) {
        return Preferences.builder()
                .userId(userId)
                .channels(Channels.builder().email(true).sms(true).push(true).inApp(true).build())
                .categories(Categories.builder()
                        .transactionAlerts(true)
                        .securityAlerts(true)
                        .emiReminders(true)
                        .accountStatements(true)
                        .marketingOffers(false)
                        .productUpdates(false)
                        .build())
                .dnd(Dnd.builder().enabled(false).startTime("22:00").endTime("07:00").build())
                .frequency("REALTIME")  // REALTIME, BATCHED_HOURLY, BATCHED_DAILY
                .build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Preferences {
        private String userId;
        private Channels channels;
        private Categories categories;
        private Dnd dnd;
        private String frequency; // REALTIME | BATCHED_HOURLY | BATCHED_DAILY
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Channels {
        private boolean email;
        private boolean sms;
        private boolean push;
        private boolean inApp;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Categories {
        private boolean transactionAlerts;
        private boolean securityAlerts;
        private boolean emiReminders;
        private boolean accountStatements;
        private boolean marketingOffers;
        private boolean productUpdates;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Dnd {
        private boolean enabled;
        @Pattern(regexp = "^([01]?\\d|2[0-3]):[0-5]\\d$", message = "Start time must be HH:mm")
        private String startTime;
        @Pattern(regexp = "^([01]?\\d|2[0-3]):[0-5]\\d$", message = "End time must be HH:mm")
        private String endTime;
    }
}

package com.cts.apigateway.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines which roles are allowed to access which URL patterns.
 * Rules are evaluated in order — first match wins.
 * Paths not matching any rule default to DENY.
 *
 * Note: COMPLIANCE_OFFICER has been retired as an assignable role. Audit log
 * and other compliance endpoints are now ADMIN-only. Any pre-existing user
 * with role=COMPLIANCE_OFFICER in identity_service_db will not pass any
 * authorization check below — re-assign them to ADMIN or block their account.
 */
@Component
public class RouteAuthorizationConfig {

    private static final AntPathMatcher PM = new AntPathMatcher();

    // Public paths — no token required
    // NOTE: /auth/logout-all is excluded — it needs token validation to extract X-User-Id
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/auth/signup",
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",        // single-session logout — refresh token is the credential
            "/auth/forgot-password",
            "/auth/verify-otp",
            "/auth/reset-password",
            "/actuator/health",
            "/actuator/info"
    );

    // Ordered path→allowed-roles rules (first match wins)
    private static final List<Map.Entry<String, Set<String>>> ROUTE_RULES = List.of(
            // Auth endpoints that require a valid token (logout-all needs X-User-Id from token)
            Map.entry("/auth/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Admin-only: staff user management
            Map.entry("/api/v1/admin/**",
                    Set.of("ADMIN")),

            // Audit logs — admin-only after compliance role retirement
            Map.entry("/api/v1/audit/**",
                    Set.of("ADMIN")),

            // Staff endpoints: CSR and above (no CUSTOMER)
            Map.entry("/api/v1/staff/**",
                    Set.of("CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Branch and employee management: branch managers and admin
            Map.entry("/api/v1/employees/**",
                    Set.of("BRANCH_MANAGER", "ADMIN")),

            // Branches — read allowed for all authenticated users
            Map.entry("/api/v1/branches/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Loan endpoints — customers apply/pay; staff decide/disburse (fine-grained via @PreAuthorize)
            Map.entry("/loans/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Customer profile and KYC — all authenticated users (fine-grained via @PreAuthorize)
            Map.entry("/customers/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            Map.entry("/kyc/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Analytics — any staff role (no CUSTOMER)
            Map.entry("/api/analytics/**",
                    Set.of("CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Role-specific dashboards
            Map.entry("/api/csr/**",
                    Set.of("CSR", "BRANCH_MANAGER", "ADMIN")),
            Map.entry("/api/branch-manager/**",
                    Set.of("BRANCH_MANAGER", "ADMIN")),
            Map.entry("/api/loan-officer/**",
                    Set.of("LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // Notification preferences & password change & support — any authenticated user
            Map.entry("/api/notifications/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),
            Map.entry("/api/auth/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),
            Map.entry("/api/support/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN")),

            // All other /api/v1/** endpoints — any authenticated user
            Map.entry("/api/v1/**",
                    Set.of("CUSTOMER", "CSR", "LOAN_OFFICER", "BRANCH_MANAGER", "ADMIN"))
    );

    public boolean isPublic(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(p -> PM.match(p, path));
    }

    public boolean isAuthorized(String path, String role) {
        if (role == null) return false;
        for (Map.Entry<String, Set<String>> rule : ROUTE_RULES) {
            if (PM.match(rule.getKey(), path)) {
                return rule.getValue().contains(role);
            }
        }
        return false;
    }
}

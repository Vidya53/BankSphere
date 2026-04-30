package com.cts.accountservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extracts user context from request headers instead of JWT.
 * This allows testing without security enabled.
 * Headers: X-User-Id, X-Username, X-Role, X-Branch-Code, X-Customer-Name, X-Email, X-Phone
 *
 * When security is re-enabled later, controllers will switch back to @AuthenticationPrincipal.
 */
@Component
public class UserContextExtractor {

    public UserContext extract(HttpServletRequest request) {
        return new UserContext(
                getOrDefault(request, "X-User-Id", "CUST001"),
                getOrDefault(request, "X-Username", "testuser"),
                getOrDefault(request, "X-Role", "CUSTOMER"),
                getOrDefault(request, "X-Branch-Code", "BR001"),
                getOrDefault(request, "X-Customer-Name", "Test User"),
                getOrDefault(request, "X-Email", "test@email.com"),
                getOrDefault(request, "X-Phone", "9876543210")
        );
    }

    private String getOrDefault(HttpServletRequest request, String header, String defaultValue) {
        String value = request.getHeader(header);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}


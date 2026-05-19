package com.cts.accountservice.context;

import com.cts.accountservice.exception.MissingGatewayHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extracts authenticated user context from request headers injected by the
 * API Gateway after JWT validation. A missing required header means the
 * request bypassed the gateway and must be rejected.
 */
@Component
public class UserContextExtractor {

    public static final String HEADER_USER_ID     = "X-User-Id";
    public static final String HEADER_USERNAME    = "X-Username";
    public static final String HEADER_ROLE        = "X-Role";
    public static final String HEADER_BRANCH_CODE = "X-Branch-Code";
    public static final String HEADER_NAME        = "X-Customer-Name";
    public static final String HEADER_EMAIL       = "X-Email";
    public static final String HEADER_PHONE       = "X-Phone";

    public UserContext extract(HttpServletRequest request) {
        return new UserContext(
                requireHeader(request, HEADER_USER_ID),
                getOptional(request, HEADER_USERNAME),
                requireHeader(request, HEADER_ROLE),
                getOptional(request, HEADER_BRANCH_CODE),
                getOptional(request, HEADER_NAME),
                getOptional(request, HEADER_EMAIL),
                getOptional(request, HEADER_PHONE)
        );
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new MissingGatewayHeaderException(
                    "Required gateway header '" + name + "' is missing. "
                            + "Ensure the request passes through the API Gateway.");
        }
        return value;
    }

    private String getOptional(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return (value != null && !value.isBlank()) ? value : null;
    }
}

package com.cts.accountservice.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Authenticated caller's user context, materialised from gateway-injected
 * request headers (X-User-Id, X-Role, X-Branch-Code, X-Email, X-Customer-Name,
 * X-Phone). Built once per request by {@link UserContextExtractor} and passed
 * down into service-layer audit + notification payloads.
 */
@Getter
@AllArgsConstructor
public class UserContext {

    private String userId;
    private String username;
    private String role;
    private String branchCode;
    private String customerName;
    private String email;
    private String phone;
}

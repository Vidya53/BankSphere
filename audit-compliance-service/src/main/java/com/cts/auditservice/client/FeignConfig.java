package com.cts.auditservice.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the gateway-injected user context headers (X-User-Id, X-Role,
 * X-Branch-Code, X-Correlation-ID) onto Feign calls so downstream services'
 * HeaderAuthenticationFilter passes and @PreAuthorize checks evaluate correctly.
 */
@Configuration
public class FeignConfig {

    private static final String[] PROPAGATED_HEADERS = {
            "X-User-Id", "X-Username", "X-Role",
            "X-Branch-Code", "X-Email", "X-Customer-Name",
            "X-Correlation-ID",
    };

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            for (String name : PROPAGATED_HEADERS) {
                String value = attrs.getRequest().getHeader(name);
                if (value != null && !value.isBlank()) {
                    template.header(name, value);
                }
            }
        };
    }
}

package com.cts.accountservice.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Default Feign configuration applied to all @FeignClient instances via
 * @EnableFeignClients(defaultConfiguration = GlobalFeignConfig.class).
 *
 * This class must NOT be annotated with @Configuration — that would make it
 * a global Spring bean and double-apply the interceptors. Feign's own
 * configuration mechanism instantiates it independently per client.
 *
 * Propagates:
 * - X-Correlation-ID (from MDC — set by CorrelationIdFilter)
 * - X-User-Id, X-Role, X-Branch-Code (from the current HTTP request headers —
 *   injected by the gateway so downstream services can enforce RBAC)
 */
@Slf4j
public class GlobalFeignConfig {

    @Bean
    public RequestInterceptor headerPropagationInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }

            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                propagateIfPresent(template, attrs, "X-User-Id");
                propagateIfPresent(template, attrs, "X-Role");
                propagateIfPresent(template, attrs, "X-Branch-Code");
            }
        };
    }

    private void propagateIfPresent(feign.RequestTemplate template,
                                     ServletRequestAttributes attrs,
                                     String headerName) {
        String value = attrs.getRequest().getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}

package com.cts.customerservices.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

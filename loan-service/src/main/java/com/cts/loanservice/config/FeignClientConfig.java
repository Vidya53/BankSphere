package com.cts.loanservice.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import feign.Response;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Slf4j
    public static class FeignErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            log.error("Feign client error: {} - Status: {}", methodKey, response.status());
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}


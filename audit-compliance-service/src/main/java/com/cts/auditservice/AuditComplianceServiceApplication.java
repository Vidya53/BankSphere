package com.cts.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.cts.auditservice.client.FeignConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class AuditComplianceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditComplianceServiceApplication.class, args);
    }
}

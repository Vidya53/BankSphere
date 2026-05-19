package com.cts.customerservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.cts.customerservices.config.GlobalFeignConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = GlobalFeignConfig.class)
@EnableDiscoveryClient
public class CustomerServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServicesApplication.class, args);
    }

}

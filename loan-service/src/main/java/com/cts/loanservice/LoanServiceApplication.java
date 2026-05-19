package com.cts.loanservice;

import com.cts.loanservice.config.GlobalFeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = GlobalFeignConfig.class)
@ComponentScan(basePackages = {"com.cts.loanservice"})
@EnableDiscoveryClient
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}

package com.cts.identityservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling    // required for @Scheduled refresh-token cleanup in RefreshTokenServiceImpl
@EnableAsync         // required for @Async on PasswordResetMailService.sendOtp — keeps the HTTP response fast while SMTP runs in the background
@EnableConfigurationProperties
@EnableDiscoveryClient
public class    IdentityServicesApplication {

    public static void main(String[] args)
    {
        SpringApplication.run(IdentityServicesApplication.class, args);
    }

}

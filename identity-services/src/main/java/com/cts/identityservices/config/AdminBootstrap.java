package com.cts.identityservices.config;

import com.cts.identityservices.entity.Role;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

/**
 * Seeds a default ADMIN user the first time the service starts so the rest of
 * the staff (CSRs, branch managers, loan officers, compliance officers) can be
 * created through the regular admin API.
 *
 * The credentials are configurable via application.yaml / env vars so they can
 * be overridden in production:
 *
 *   admin.bootstrap.email=admin@banksphere.com
 *   admin.bootstrap.password=Admin@12345
 *
 * Disable seeding entirely with admin.bootstrap.enabled=false.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${admin.bootstrap.email:admin@banksphere.com}")
    private String email;

    @Value("${admin.bootstrap.password:Admin@12345}")
    private String password;

    @Value("${admin.bootstrap.fullName:System Administrator}")
    private String fullName;

    @Value("${admin.bootstrap.phoneNumber:9999999999}")
    private String phoneNumber;

    @Bean
    public CommandLineRunner seedAdminUser() {
        return args -> {
            if (!enabled) {
                log.info("Admin bootstrap disabled (admin.bootstrap.enabled=false).");
                return;
            }
            if (userRepository.existsByEmail(email)) {
                log.info("Admin bootstrap: '{}' already exists, skipping.", email);
                return;
            }
            User admin = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .phoneNumber(phoneNumber)
                    .dateOfBirth(LocalDate.of(1985, 1, 1))
                    .role(Role.ADMIN)
                    .build();
            try {
                userRepository.save(admin);
                log.info("══════════════════════════════════════════════════════════════════");
                log.info(" Default ADMIN created — email={} password={}", email, password);
                log.info(" Sign in through the gateway at http://localhost:8090/auth/login");
                log.info(" Change this password immediately in any non-dev environment.");
                log.info("══════════════════════════════════════════════════════════════════");
            } catch (Exception e) {
                log.warn("Admin bootstrap could not seed default admin: {}", e.getMessage());
            }
        };
    }
}

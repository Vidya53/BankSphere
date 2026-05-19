package com.cts.transactionservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full Spring Boot context smoke test.
 *
 * Disabled by default because the application's bean graph reaches out to:
 *   - Config Server (jwt.secret, route config, datasource overrides)
 *   - MySQL on localhost:3306
 *   - Eureka on localhost:8761
 *
 * Enable this once those services (or a test-containers / embedded equivalent)
 * are available in the test environment. The focused JUnit + Mockito tests
 * under `service`, `controller`, and `exception` packages do NOT require any
 * of these — they run on every `mvn test`.
 */
@SpringBootTest
@Disabled("Full context load requires MySQL / Config Server / Eureka. Run via `mvn test -Dgroups=integration` once infra is available.")
class TransactionServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — re-enable this class once integration infra is wired in.
    }
}

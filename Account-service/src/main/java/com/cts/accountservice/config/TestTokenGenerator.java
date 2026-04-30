package com.cts.accountservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Generates test JWT tokens at startup for Postman/Swagger testing.
 * Remove this in production.
 */
@Component
@Slf4j
@Order(2)
public class TestTokenGenerator implements CommandLineRunner {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public void run(String... args) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        long expiry = 1000L * 60 * 60 * 24; // 24 hours

        String customerToken = Jwts.builder()
                .subject("rajesh.kumar")
                .claim("userId", "CUST001")
                .claim("role", "CUSTOMER")
                .claim("branchCode", "BR001")
                .claim("customerName", "Rajesh Kumar")
                .claim("email", "rajesh.kumar@email.com")
                .claim("phone", "9876543210")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();

        String csrToken = Jwts.builder()
                .subject("staff.csr01")
                .claim("userId", "STAFF001")
                .claim("role", "CSR")
                .claim("branchCode", "BR001")
                .claim("customerName", "CSR Staff One")
                .claim("email", "csr01@banksphere.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();

        String managerToken = Jwts.builder()
                .subject("staff.manager01")
                .claim("userId", "MGR001")
                .claim("role", "BRANCH_MANAGER")
                .claim("branchCode", "BR001")
                .claim("customerName", "Branch Manager One")
                .claim("email", "manager01@banksphere.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();

        String customer2Token = Jwts.builder()
                .subject("deepika.patel")
                .claim("userId", "CUST004")
                .claim("role", "CUSTOMER")
                .claim("branchCode", "BR001")
                .claim("customerName", "Deepika Patel")
                .claim("email", "deepika.patel@email.com")
                .claim("phone", "9876543240")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();

        log.info("\n\n========== TEST JWT TOKENS (valid 24h) ==========");
        log.info("\n[CUSTOMER - Rajesh Kumar (CUST001, BR001)]:\nBearer {}", customerToken);
        log.info("\n[CUSTOMER - Deepika Patel (CUST004, BR001)]:\nBearer {}", customer2Token);
        log.info("\n[CSR - Staff (STAFF001, BR001)]:\nBearer {}", csrToken);
        log.info("\n[BRANCH_MANAGER - Manager (MGR001, BR001)]:\nBearer {}", managerToken);
        log.info("\n==================================================\n");
    }
}


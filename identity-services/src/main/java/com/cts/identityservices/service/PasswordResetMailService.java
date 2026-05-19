package com.cts.identityservices.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends the password-reset OTP via SMTP.
 *
 * `JavaMailSender` is injected via {@link ObjectProvider} so the bean is
 * optional — Spring Boot only auto-creates it when {@code spring.mail.host}
 * is set at startup. If it isn't (dev environment without SMTP creds, or
 * config-server unavailable), this service falls back to logging the OTP at
 * ERROR level so developers can still complete the flow.
 *
 * In production, set SMTP_HOST / SMTP_USERNAME / SMTP_PASSWORD via env vars
 * and the fallback branch never fires.
 */
@Service
@Slf4j
public class PasswordResetMailService {

    private final JavaMailSender mailSender;   // may be null when SMTP isn't configured

    @Value("${password-reset.mail-from:noreply@banksphere.com}")
    private String from;

    @Value("${password-reset.mail-from-name:BankSphere}")
    private String fromName;

    @Value("${password-reset.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        if (this.mailSender == null) {
            log.warn("No JavaMailSender bean available (spring.mail.host not set). " +
                     "Password-reset OTPs will be logged to the console only — " +
                     "DEV MODE. Configure SMTP_HOST / SMTP_USERNAME / SMTP_PASSWORD for production.");
        }
    }

    /**
     * Sends the OTP asynchronously so the /auth/forgot-password HTTP response
     * returns immediately. Without @Async, the SMTP connection attempt (up to
     * 5s with the default timeout) blocks the response thread — long enough
     * for the gateway's Resilience4j TimeLimiter to fire and return its
     * SERVICE_UNAVAILABLE fallback, even though the OTP was generated and
     * stored just fine.
     */
    @Async
    public void sendOtp(String to, String fullName, String otp) {
        String subject = "[BankSphere] Your password reset code";
        String body    = buildBody(fullName, otp);

        if (mailSender == null) {
            log.error("Mail not configured — FOR DEV USE ONLY: OTP for {} is: {}", to, otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + from + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Password-reset OTP emailed to {}", to);
        } catch (Exception e) {
            // SMTP misconfigured or mail relay down — log the OTP so dev
            // testing isn't blocked. Never log it at this level in production
            // with a real mail relay.
            log.error("Password-reset OTP could not be emailed to {} ({}). " +
                      "FOR DEV USE ONLY — OTP is: {}",
                      to, e.getMessage(), otp);
        }
    }

    private String buildBody(String fullName, String otp) {
        String greet = (fullName == null || fullName.isBlank()) ? "Hi" : "Hi " + fullName;
        return greet + ",\n\n"
                + "Someone requested a password reset for your BankSphere account.\n\n"
                + "Your one-time verification code is:\n\n"
                + "    " + otp + "\n\n"
                + "This code expires in " + otpExpiryMinutes + " minutes. If you didn't request a\n"
                + "reset, you can ignore this email and your password will stay the same.\n\n"
                + "— BankSphere security\n";
    }
}

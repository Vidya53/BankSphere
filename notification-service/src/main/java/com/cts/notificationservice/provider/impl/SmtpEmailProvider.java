package com.cts.notificationservice.provider.impl;

import com.cts.notificationservice.provider.EmailProvider;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from:noreply@banksphere.com}")
    private String fromAddress;

    @Value("${notification.mail.from-name:BankSphere}")
    private String fromName;

    @Override
    public void send(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);  // true = isHtml

        mailSender.send(message);
        log.debug("Email sent: to={} subject={}", to, subject);
    }
}

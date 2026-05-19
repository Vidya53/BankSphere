package com.cts.notificationservice.provider.impl;

import com.cts.notificationservice.provider.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Twilio SMS provider stub.
 *
 * To activate, add the Twilio SDK to pom.xml:
 *   <dependency>
 *       <groupId>com.twilio.sdk</groupId>
 *       <artifactId>twilio</artifactId>
 *       <version>9.x.x</version>
 *   </dependency>
 *
 * Then replace the log statement below with:
 *   Twilio.init(accountSid, authToken);
 *   Message.creator(new PhoneNumber(toPhone), new PhoneNumber(fromPhone), message).create();
 *
 * For AWS SNS instead of Twilio:
 *   SnsClient.create().publish(r -> r.phoneNumber(toPhone).message(message));
 */
@Component
@Slf4j
public class TwilioSmsProvider implements SmsProvider {

    @Value("${notification.sms.from:+0000000000}")
    private String fromPhone;

    @Value("${notification.sms.account-sid:#{null}}")
    private String accountSid;

    @Value("${notification.sms.auth-token:#{null}}")
    private String authToken;

    @Override
    public void send(String toPhone, String message) throws Exception {
        if (toPhone == null || toPhone.isBlank()) {
            log.warn("SMS skipped — no phone number provided");
            return;
        }
        // TODO: Twilio.init(accountSid, authToken); Message.creator(...).create();
        log.info("[SMS-STUB] to={} message={}", toPhone, message);
    }
}

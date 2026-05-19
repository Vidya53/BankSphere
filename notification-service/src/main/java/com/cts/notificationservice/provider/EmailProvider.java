package com.cts.notificationservice.provider;

public interface EmailProvider {
    void send(String to, String subject, String htmlBody) throws Exception;
}

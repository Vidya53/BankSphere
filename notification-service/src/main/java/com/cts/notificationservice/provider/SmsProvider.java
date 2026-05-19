package com.cts.notificationservice.provider;

public interface SmsProvider {
    void send(String toPhone, String message) throws Exception;
}

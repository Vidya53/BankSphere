package com.cts.notificationservice.provider;

public interface PushProvider {
    void send(String userId, String title, String body) throws Exception;
}

package com.cts.accountservice.service;

public interface NotificationService {

    void sendNotification(String userId, String email, String subject, String message);
}

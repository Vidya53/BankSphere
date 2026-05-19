package com.cts.notificationservice.enums;

public enum NotificationPriority {
    HIGH,    // OTP, fraud alerts, account blocked — bypasses rate limiting and DND
    NORMAL,  // account status changes, application updates
    LOW      // promotional, balance summary, reminders
}

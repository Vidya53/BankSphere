package com.cts.notificationservice.enums;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,   // user opted out or DND window active
    DLT        // moved to dead-letter topic after exhausting retries
}

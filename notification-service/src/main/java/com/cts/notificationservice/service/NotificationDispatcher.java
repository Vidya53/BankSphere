package com.cts.notificationservice.service;

import com.cts.notificationservice.dto.NotificationEventMessage;
import com.cts.notificationservice.entity.NotificationLog;

import java.util.List;

public interface NotificationDispatcher {

    void dispatch(NotificationEventMessage message);

    List<NotificationLog> retryFailed(int batchSize);
}

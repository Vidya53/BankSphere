package com.cts.notificationservice.provider.impl;

import com.cts.notificationservice.provider.PushProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firebase Cloud Messaging (FCM) push notification stub.
 *
 * To activate, add the Firebase Admin SDK to pom.xml:
 *   <dependency>
 *       <groupId>com.google.firebase</groupId>
 *       <artifactId>firebase-admin</artifactId>
 *       <version>9.x.x</version>
 *   </dependency>
 *
 * Then initialize FirebaseApp with a service account JSON and send:
 *   Message msg = Message.builder()
 *       .setToken(deviceToken)
 *       .setNotification(Notification.builder().setTitle(title).setBody(body).build())
 *       .build();
 *   FirebaseMessaging.getInstance().send(msg);
 *
 * Device tokens must be stored in a user-device registry table (not implemented here).
 * Consider adding a DeviceToken entity to store FCM tokens registered by mobile clients.
 */
@Component
@Slf4j
public class FcmPushProvider implements PushProvider {

    @Value("${notification.push.enabled:false}")
    private boolean pushEnabled;

    @Override
    public void send(String userId, String title, String body) throws Exception {
        if (!pushEnabled) {
            log.debug("Push notifications disabled — skipping for userId={}", userId);
            return;
        }
        // TODO: look up FCM device tokens for userId, then call FirebaseMessaging
        log.info("[PUSH-STUB] userId={} title={}", userId, title);
    }
}

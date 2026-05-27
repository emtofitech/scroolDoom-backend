package com.scrolldoom.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendBreachNotification(String fcmToken, String appLabel,
                                       int actualMinutes, int limitMinutes,
                                       String partnerDisplayName) {
        String body = partnerDisplayName + " went over their " + appLabel
                + " limit \u2014 " + actualMinutes + " min used / "
                + limitMinutes + " min allowed";

        sendBreachNotification(fcmToken, "ScrollDoom Alert \u26A0\uFE0F", body);
    }

    public void sendBreachNotification(String fcmToken, String title, String body) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM notification sent successfully: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token {}: {}", fcmToken, e.getMessage());
        }
    }
}

package com.scrolldoom.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.scrolldoom.model.NotificationDelivery;
import com.scrolldoom.repository.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public NotificationService(NotificationDeliveryRepository notificationDeliveryRepository) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
    }

    public void sendBreachNotification(String fcmToken, String recipientFirebaseUid,
                                       String appLabel, int actualMinutes, int limitMinutes,
                                       String partnerDisplayName) {
        String body = partnerDisplayName + " went over their " + appLabel
                + " limit \u2014 " + actualMinutes + " min used / "
                + limitMinutes + " min allowed";

        sendBreachNotification(fcmToken, recipientFirebaseUid, "ScrollDoom Alert \u26A0\uFE0F", body);
    }

    public void sendBreachNotification(String fcmToken, String recipientFirebaseUid,
                                       String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized - skipping notification");
            return;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("No FCM token provided - skipping notification");
            return;
        }

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
            
            NotificationDelivery delivery = NotificationDelivery.builder()
                    .firebaseUid(recipientFirebaseUid)
                    .notificationType("breach")
                    .title(title)
                    .body(body)
                    .delivered(true)
                    .sentAt(new Date())
                    .deliveredAt(new Date())
                    .build();
            notificationDeliveryRepository.save(delivery);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token {}: {}", fcmToken, e.getMessage());
            
            NotificationDelivery delivery = NotificationDelivery.builder()
                    .firebaseUid(recipientFirebaseUid)
                    .notificationType("breach")
                    .title(title)
                    .body(body)
                    .delivered(false)
                    .failureReason(e.getMessage())
                    .sentAt(new Date())
                    .build();
            notificationDeliveryRepository.save(delivery);
        }
    }
}

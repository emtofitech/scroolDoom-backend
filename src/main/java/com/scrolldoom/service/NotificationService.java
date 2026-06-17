package com.scrolldoom.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.scrolldoom.dto.NotificationDeliveryResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.NotificationDelivery;
import com.scrolldoom.model.NotificationPreference;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.NotificationDeliveryRepository;
import com.scrolldoom.repository.NotificationPreferenceRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationDeliveryRepository notificationDeliveryRepository,
                               NotificationPreferenceRepository notificationPreferenceRepository,
                               UserRepository userRepository) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userRepository = userRepository;
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

    public Page<NotificationDeliveryResponse> listNotifications(ObjectId userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return notificationDeliveryRepository
                .findByFirebaseUid(user.getFirebaseUid(), pageable)
                .map(this::mapToResponse);
    }

    public long getUnreadCount(ObjectId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationDeliveryRepository.countByFirebaseUidAndOpenedFalse(user.getFirebaseUid());
    }

    public void markAllAsRead(ObjectId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<NotificationDelivery> unread = notificationDeliveryRepository
                .findByFirebaseUidAndOpenedFalseOrderBySentAtDesc(user.getFirebaseUid());
        Date now = new Date();
        unread.forEach(n -> { n.setOpened(true); n.setOpenedAt(now); });
        notificationDeliveryRepository.saveAll(unread);
    }

    public NotificationPreference getPreferences(ObjectId userId) {
        return getOrCreatePreferences(userId);
    }

    public NotificationPreference updatePreferences(ObjectId userId, Map<String, Boolean> updates) {
        NotificationPreference prefs = getOrCreatePreferences(userId);
        if (updates.containsKey("breachAlerts")) prefs.setBreachAlerts(updates.get("breachAlerts"));
        if (updates.containsKey("streakBroken")) prefs.setStreakBroken(updates.get("streakBroken"));
        if (updates.containsKey("appLocked")) prefs.setAppLocked(updates.get("appLocked"));
        return notificationPreferenceRepository.save(prefs);
    }

    private NotificationPreference getOrCreatePreferences(ObjectId userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> notificationPreferenceRepository.save(
                        NotificationPreference.builder().userId(userId).build()));
    }

    private NotificationDeliveryResponse mapToResponse(NotificationDelivery delivery) {
        return NotificationDeliveryResponse.builder()
                .id(delivery.getId() != null ? delivery.getId().toHexString() : null)
                .notificationType(delivery.getNotificationType())
                .title(delivery.getTitle())
                .body(delivery.getBody())
                .delivered(delivery.isDelivered())
                .opened(delivery.isOpened())
                .sentAt(delivery.getSentAt())
                .deliveredAt(delivery.getDeliveredAt())
                .openedAt(delivery.getOpenedAt())
                .build();
    }
}

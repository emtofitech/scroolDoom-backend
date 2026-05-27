package com.scrolldoom.service;

import com.scrolldoom.model.AppUsageEvent;
import com.scrolldoom.model.NotificationDelivery;
import com.scrolldoom.model.UserActivity;
import com.scrolldoom.repository.AppUsageEventRepository;
import com.scrolldoom.repository.NotificationDeliveryRepository;
import com.scrolldoom.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsagePollingService {

    private final AppUsageEventRepository appUsageEventRepository;
    private final UserActivityRepository userActivityRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final MongoTemplate mongoTemplate;

    private static final long OFFLINE_THRESHOLD_MS = 30 * 60 * 1000;

    @Scheduled(fixedRate = 1800000)
    public void pollUsageData() {
        log.info("Running usage polling task at {}", new Date());
        
        updateOnlineStatuses();
        cleanupStaleSessions();
        logUsageSummary();
    }

    private void updateOnlineStatuses() {
        Date threshold = new Date(System.currentTimeMillis() - OFFLINE_THRESHOLD_MS);
        
        Query query = new Query()
            .addCriteria(Criteria.where("online").is(true))
            .addCriteria(Criteria.where("lastSeen").lt(threshold));
        
        Update update = new Update()
            .set("online", false)
            .set("updatedAt", new Date());
        
        long count = mongoTemplate.updateMulti(query, update, UserActivity.class).getModifiedCount();
        log.info("Marked {} users as offline", count);
    }

    private void cleanupStaleSessions() {
        Date threshold = new Date(System.currentTimeMillis() - OFFLINE_THRESHOLD_MS);
        
        List<UserActivity> staleUsers = userActivityRepository.findByLastSeenAfter(threshold);
        for (UserActivity activity : staleUsers) {
            if (activity.isOnline() && activity.getLastSeen().before(threshold)) {
                activity.setOnline(false);
                activity.setUpdatedAt(new Date());
                userActivityRepository.save(activity);
            }
        }
    }

    private void logUsageSummary() {
        long totalEvents = appUsageEventRepository.count();
        long onlineUsers = userActivityRepository.countByOnlineTrue();
        long activeToday = userActivityRepository.countByLastSeenAfter(
            new Date(System.currentTimeMillis() - 86400000)
        );
        long notificationsSent = notificationDeliveryRepository.count();
        long notificationsDelivered = notificationDeliveryRepository.countByDelivered(true);
        long notificationsOpened = notificationDeliveryRepository.countByOpened(true);

        log.info("Usage Summary - Online: {}, Active Today: {}, Total Events: {}", 
            onlineUsers, activeToday, totalEvents);
        log.info("Notification Stats - Sent: {}, Delivered: {}, Opened: {}", 
            notificationsSent, notificationsDelivered, notificationsOpened);
    }

    public AppUsageEvent trackEvent(String firebaseUid, String eventType, String screenName, 
                                     String featureName, long durationMs, String deviceInfo, String appVersion) {
        AppUsageEvent event = AppUsageEvent.builder()
            .firebaseUid(firebaseUid)
            .eventType(eventType)
            .screenName(screenName)
            .featureName(featureName)
            .durationMs(durationMs)
            .deviceInfo(deviceInfo)
            .appVersion(appVersion)
            .timestamp(new Date())
            .build();
        
        event = appUsageEventRepository.save(event);
        updateLastSeen(firebaseUid, screenName, featureName);
        
        return event;
    }

    public UserActivity updateLastSeen(String firebaseUid, String screenName, String featureName) {
        Optional<UserActivity> existing = userActivityRepository.findByFirebaseUid(firebaseUid);
        
        UserActivity activity;
        if (existing.isPresent()) {
            activity = existing.get();
            activity.setOnline(true);
            activity.setLastScreen(screenName);
            activity.setLastFeature(featureName);
            activity.setLastSeen(new Date());
            activity.setUpdatedAt(new Date());
        } else {
            activity = UserActivity.builder()
                .firebaseUid(firebaseUid)
                .online(true)
                .lastScreen(screenName)
                .lastFeature(featureName)
                .lastSeen(new Date())
                .lastAppOpen(new Date())
                .totalSessionMs(0)
                .sessionCount(1)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        }
        
        return userActivityRepository.save(activity);
    }

    public UserActivity recordAppOpen(String firebaseUid) {
        Optional<UserActivity> existing = userActivityRepository.findByFirebaseUid(firebaseUid);
        
        UserActivity activity;
        if (existing.isPresent()) {
            activity = existing.get();
            activity.setOnline(true);
            activity.setLastAppOpen(new Date());
            activity.setLastSeen(new Date());
            activity.setSessionCount(activity.getSessionCount() + 1);
            activity.setUpdatedAt(new Date());
        } else {
            activity = UserActivity.builder()
                .firebaseUid(firebaseUid)
                .online(true)
                .lastAppOpen(new Date())
                .lastSeen(new Date())
                .totalSessionMs(0)
                .sessionCount(1)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        }
        
        return userActivityRepository.save(activity);
    }

    public UserActivity recordAppClose(String firebaseUid) {
        Optional<UserActivity> existing = userActivityRepository.findByFirebaseUid(firebaseUid);
        
        if (existing.isPresent()) {
            UserActivity activity = existing.get();
            activity.setOnline(false);
            activity.setLastAppClose(new Date());
            activity.setUpdatedAt(new Date());
            
            if (activity.getLastAppOpen() != null) {
                long sessionDuration = System.currentTimeMillis() - activity.getLastAppOpen().getTime();
                activity.setTotalSessionMs(activity.getTotalSessionMs() + sessionDuration);
            }
            
            return userActivityRepository.save(activity);
        }
        
        return null;
    }

    public NotificationDelivery trackNotification(String firebaseUid, String notificationType, 
                                                   String title, String body) {
        NotificationDelivery delivery = NotificationDelivery.builder()
            .firebaseUid(firebaseUid)
            .notificationType(notificationType)
            .title(title)
            .body(body)
            .delivered(false)
            .opened(false)
            .sentAt(new Date())
            .build();
        
        return notificationDeliveryRepository.save(delivery);
    }

    public NotificationDelivery markDelivered(String deliveryId) {
        Optional<NotificationDelivery> delivery = notificationDeliveryRepository.findById(
            new org.bson.types.ObjectId(deliveryId)
        );
        
        if (delivery.isPresent()) {
            NotificationDelivery d = delivery.get();
            d.setDelivered(true);
            d.setDeliveredAt(new Date());
            return notificationDeliveryRepository.save(d);
        }
        
        return null;
    }

    public NotificationDelivery markOpened(String deliveryId) {
        Optional<NotificationDelivery> delivery = notificationDeliveryRepository.findById(
            new org.bson.types.ObjectId(deliveryId)
        );
        
        if (delivery.isPresent()) {
            NotificationDelivery d = delivery.get();
            d.setOpened(true);
            d.setOpenedAt(new Date());
            return notificationDeliveryRepository.save(d);
        }
        
        return null;
    }

    public Map<String, Object> getUserUsageStats(String firebaseUid) {
        Map<String, Object> stats = new HashMap<>();
        
        Optional<UserActivity> activity = userActivityRepository.findByFirebaseUid(firebaseUid);
        if (activity.isPresent()) {
            UserActivity a = activity.get();
            stats.put("online", a.isOnline());
            stats.put("lastSeen", a.getLastSeen());
            stats.put("lastScreen", a.getLastScreen());
            stats.put("sessionCount", a.getSessionCount());
            stats.put("totalSessionMs", a.getTotalSessionMs());
        }
        
        long totalEvents = appUsageEventRepository.countByFirebaseUidAndEventType(firebaseUid, "screen_view");
        stats.put("totalScreenViews", totalEvents);
        
        List<NotificationDelivery> notifications = notificationDeliveryRepository
            .findByFirebaseUidOrderBySentAtDesc(firebaseUid);
        stats.put("totalNotifications", notifications.size());
        stats.put("notificationsOpened", notifications.stream().filter(NotificationDelivery::isOpened).count());
        
        return stats;
    }

    public Map<String, Object> getGlobalUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("onlineUsers", userActivityRepository.countByOnlineTrue());
        stats.put("activeToday", userActivityRepository.countByLastSeenAfter(
            new Date(System.currentTimeMillis() - 86400000)
        ));
        stats.put("activeThisWeek", userActivityRepository.countByLastSeenAfter(
            new Date(System.currentTimeMillis() - 604800000)
        ));
        
        stats.put("totalEvents", appUsageEventRepository.count());
        stats.put("eventsToday", appUsageEventRepository.countByTimestampBetween(
            new Date(System.currentTimeMillis() - 86400000), new Date()
        ));
        
        stats.put("totalNotifications", notificationDeliveryRepository.count());
        stats.put("notificationsDelivered", notificationDeliveryRepository.countByDelivered(true));
        stats.put("notificationsOpened", notificationDeliveryRepository.countByOpened(true));
        stats.put("deliveryRate", calculateDeliveryRate());
        stats.put("openRate", calculateOpenRate());
        
        return stats;
    }

    private double calculateDeliveryRate() {
        long total = notificationDeliveryRepository.count();
        if (total == 0) return 0.0;
        long delivered = notificationDeliveryRepository.countByDelivered(true);
        return (double) delivered / total * 100;
    }

    private double calculateOpenRate() {
        long delivered = notificationDeliveryRepository.countByDelivered(true);
        if (delivered == 0) return 0.0;
        long opened = notificationDeliveryRepository.countByOpened(true);
        return (double) opened / delivered * 100;
    }
}

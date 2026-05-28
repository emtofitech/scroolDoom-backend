package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.UsageEventResponse;
import com.scrolldoom.dto.UserActivityResponse;
import com.scrolldoom.dto.TrackEventRequest;
import com.scrolldoom.dto.TrackNotificationRequest;
import com.scrolldoom.dto.NotificationDeliveryResponse;
import com.scrolldoom.model.AppUsageEvent;
import com.scrolldoom.model.NotificationDelivery;
import com.scrolldoom.model.UserActivity;
import com.scrolldoom.service.UsagePollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
@Tag(name = "Usage Tracking", description = "App usage and notification tracking endpoints")
public class UsageController {

    private final UsagePollingService usagePollingService;

    @PostMapping("/events")
    @Operation(summary = "Track app usage event")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<UsageEventResponse>> trackEvent(
            Authentication authentication,
            @Valid @RequestBody TrackEventRequest request) {
        String firebaseUid = authentication.getName();
        AppUsageEvent event = usagePollingService.trackEvent(
            firebaseUid,
            request.getEventType(),
            request.getScreenName(),
            request.getFeatureName(),
            request.getDurationMs(),
            request.getDeviceInfo(),
            request.getAppVersion()
        );
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToUsageEventResponse(event)));
    }

    @PostMapping("/app-open")
    @Operation(summary = "Record app open")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<UserActivityResponse>> recordAppOpen(
            Authentication authentication) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.recordAppOpen(firebaseUid);
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToActivityResponse(activity)));
    }

    @PostMapping("/app-close")
    @Operation(summary = "Record app close")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<UserActivityResponse>> recordAppClose(
            Authentication authentication) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.recordAppClose(firebaseUid);
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToActivityResponse(activity)));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Send heartbeat to maintain online status")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<UserActivityResponse>> heartbeat(
            Authentication authentication,
            @RequestParam(required = false) String screenName,
            @RequestParam(required = false) String featureName) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.updateLastSeen(firebaseUid, screenName, featureName);
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToActivityResponse(activity)));
    }

    @PostMapping("/notifications")
    @Operation(summary = "Track notification delivery")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<NotificationDeliveryResponse>> trackNotification(
            Authentication authentication,
            @Valid @RequestBody TrackNotificationRequest request) {
        String firebaseUid = authentication.getName();
        NotificationDelivery delivery = usagePollingService.trackNotification(
            firebaseUid,
            request.getNotificationType(),
            request.getTitle(),
            request.getBody()
        );
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToNotificationResponse(delivery)));
    }

    @PutMapping("/notifications/{deliveryId}/delivered")
    @Operation(summary = "Mark notification as delivered")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<NotificationDeliveryResponse>> markDelivered(
            @PathVariable String deliveryId) {
        NotificationDelivery delivery = usagePollingService.markDelivered(deliveryId);
        if (delivery == null) {
            return ResponseEntity.status(404)
                    .body(com.scrolldoom.dto.ApiEnvelope.error(404, "Notification not found"));
        }
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToNotificationResponse(delivery)));
    }

    @PutMapping("/notifications/{deliveryId}/opened")
    @Operation(summary = "Mark notification as opened")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<NotificationDeliveryResponse>> markOpened(
            @PathVariable String deliveryId) {
        NotificationDelivery delivery = usagePollingService.markOpened(deliveryId);
        if (delivery == null) {
            return ResponseEntity.status(404)
                    .body(com.scrolldoom.dto.ApiEnvelope.error(404, "Notification not found"));
        }
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToNotificationResponse(delivery)));
    }

    @GetMapping("/stats/me")
    @Operation(summary = "Get current user usage stats")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<Map<String, Object>>> getMyStats(
            Authentication authentication) {
        String firebaseUid = authentication.getName();
        Map<String, Object> stats = usagePollingService.getUserUsageStats(firebaseUid);
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(stats));
    }

    @GetMapping("/stats/global")
    @Operation(summary = "Get global usage stats (admin)")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<Map<String, Object>>> getGlobalStats() {
        Map<String, Object> stats = usagePollingService.getGlobalUsageStats();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(stats));
    }

    private UsageEventResponse mapToUsageEventResponse(AppUsageEvent event) {
        return UsageEventResponse.builder()
                .id(event.getId() != null ? event.getId().toHexString() : null)
                .eventType(event.getEventType())
                .screenName(event.getScreenName())
                .featureName(event.getFeatureName())
                .durationMs(event.getDurationMs())
                .deviceInfo(event.getDeviceInfo())
                .appVersion(event.getAppVersion())
                .timestamp(event.getTimestamp())
                .build();
    }

    private UserActivityResponse mapToActivityResponse(UserActivity activity) {
        if (activity == null) return null;
        return UserActivityResponse.builder()
                .id(activity.getId() != null ? activity.getId().toHexString() : null)
                .online(activity.isOnline())
                .lastScreen(activity.getLastScreen())
                .lastFeature(activity.getLastFeature())
                .lastSeen(activity.getLastSeen())
                .lastAppOpen(activity.getLastAppOpen())
                .lastAppClose(activity.getLastAppClose())
                .totalSessionMs(activity.getTotalSessionMs())
                .sessionCount(activity.getSessionCount())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    private NotificationDeliveryResponse mapToNotificationResponse(NotificationDelivery delivery) {
        if (delivery == null) return null;
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

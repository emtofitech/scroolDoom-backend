package com.scrolldoom.controller;

import com.scrolldoom.dto.GlobalStatsResponse;
import com.scrolldoom.dto.TrackEventRequest;
import com.scrolldoom.dto.TrackNotificationRequest;
import com.scrolldoom.dto.UsageStatsResponse;
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
    public ResponseEntity<AppUsageEvent> trackEvent(
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
        return ResponseEntity.ok(event);
    }

    @PostMapping("/app-open")
    @Operation(summary = "Record app open")
    public ResponseEntity<UserActivity> recordAppOpen(Authentication authentication) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.recordAppOpen(firebaseUid);
        return ResponseEntity.ok(activity);
    }

    @PostMapping("/app-close")
    @Operation(summary = "Record app close")
    public ResponseEntity<UserActivity> recordAppClose(Authentication authentication) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.recordAppClose(firebaseUid);
        return ResponseEntity.ok(activity);
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Send heartbeat to maintain online status")
    public ResponseEntity<UserActivity> heartbeat(
            Authentication authentication,
            @RequestParam(required = false) String screenName,
            @RequestParam(required = false) String featureName) {
        String firebaseUid = authentication.getName();
        UserActivity activity = usagePollingService.updateLastSeen(firebaseUid, screenName, featureName);
        return ResponseEntity.ok(activity);
    }

    @PostMapping("/notifications")
    @Operation(summary = "Track notification delivery")
    public ResponseEntity<NotificationDelivery> trackNotification(
            Authentication authentication,
            @Valid @RequestBody TrackNotificationRequest request) {
        String firebaseUid = authentication.getName();
        NotificationDelivery delivery = usagePollingService.trackNotification(
            firebaseUid,
            request.getNotificationType(),
            request.getTitle(),
            request.getBody()
        );
        return ResponseEntity.ok(delivery);
    }

    @PutMapping("/notifications/{deliveryId}/delivered")
    @Operation(summary = "Mark notification as delivered")
    public ResponseEntity<NotificationDelivery> markDelivered(@PathVariable String deliveryId) {
        NotificationDelivery delivery = usagePollingService.markDelivered(deliveryId);
        if (delivery == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(delivery);
    }

    @PutMapping("/notifications/{deliveryId}/opened")
    @Operation(summary = "Mark notification as opened")
    public ResponseEntity<NotificationDelivery> markOpened(@PathVariable String deliveryId) {
        NotificationDelivery delivery = usagePollingService.markOpened(deliveryId);
        if (delivery == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(delivery);
    }

    @GetMapping("/stats/me")
    @Operation(summary = "Get current user usage stats")
    public ResponseEntity<Map<String, Object>> getMyStats(Authentication authentication) {
        String firebaseUid = authentication.getName();
        Map<String, Object> stats = usagePollingService.getUserUsageStats(firebaseUid);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/global")
    @Operation(summary = "Get global usage stats (admin)")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> stats = usagePollingService.getGlobalUsageStats();
        return ResponseEntity.ok(stats);
    }
}

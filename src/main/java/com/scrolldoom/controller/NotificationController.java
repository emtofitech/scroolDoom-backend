package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.NotificationDeliveryResponse;
import com.scrolldoom.model.NotificationPreference;
import com.scrolldoom.service.NotificationService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "View and manage push notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService,
                                  UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List notifications",
            description = "Returns a paginated list of push notifications for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of notifications"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiEnvelope<Page<NotificationDeliveryResponse>>> listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ObjectId userId = userService.getCurrentUserId();
        Page<NotificationDeliveryResponse> result = notificationService.listNotifications(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt")));
        return ResponseEntity.ok(ApiEnvelope.ok(result));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiEnvelope<Map<String, Long>>> getUnreadCount() {
        ObjectId userId = userService.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiEnvelope.ok(Map.of("unreadCount", count)));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiEnvelope<Void>> markAllAsRead() {
        ObjectId userId = userService.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiEnvelope.ok(null));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get notification preferences",
            description = "Returns per-type notification toggles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification preferences returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiEnvelope<NotificationPreference>> getPreferences() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiEnvelope.ok(notificationService.getPreferences(userId)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences",
            description = "Accepts a partial map of notification type toggles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification preferences updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiEnvelope<NotificationPreference>> updatePreferences(
            @RequestBody Map<String, Boolean> updates) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiEnvelope.ok(notificationService.updatePreferences(userId, updates)));
    }
}

package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification delivery status response")
public class NotificationDeliveryResponse {

    @Schema(description = "Notification delivery ID")
    private String id;

    @Schema(description = "Notification type", example = "breach")
    private String notificationType;

    @Schema(description = "Notification title", example = "ScrollDoom Alert")
    private String title;

    @Schema(description = "Notification body")
    private String body;

    @Schema(description = "Whether the notification was delivered")
    private boolean delivered;

    @Schema(description = "Whether the notification was opened")
    private boolean opened;

    @Schema(description = "Timestamp when sent")
    private Date sentAt;

    @Schema(description = "Timestamp when delivered")
    private Date deliveredAt;

    @Schema(description = "Timestamp when opened")
    private Date openedAt;
}

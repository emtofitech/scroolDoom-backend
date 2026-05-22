package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "A recorded breach event where the user exceeded their app limit")
public class BreachEventResponse {

    @Schema(description = "MongoDB ObjectId as hex string", example = "664a1b2c3d4e5f6a7b8c9d0e")
    private String id;

    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;

    @Schema(description = "Daily limit in minutes that was set", example = "30")
    private int limitMinutes;

    @Schema(description = "Actual minutes used", example = "45")
    private int actualMinutes;

    @Schema(description = "Whether the partner was notified via FCM")
    private boolean partnerNotified;

    @Schema(description = "Timestamp when the breach occurred")
    private Date breachedAt;
}

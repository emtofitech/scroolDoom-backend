package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Daily limit status for an app, indicating whether it has been exceeded")
public class LimitStatusResponse {

    @Schema(description = "AppLimit MongoDB ObjectId as hex string", example = "664a1b2c3d4e5f6a7b8c9d0e")
    private String id;

    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;

    @Schema(description = "Daily time limit in minutes", example = "30")
    private int dailyLimitMinutes;

    @Schema(description = "Whether a screen-time breach has been recorded today for this app")
    private boolean exceeded;

    @Schema(description = "Actual minutes used when the breach was reported (null if not exceeded)")
    private Integer actualMinutes;

    @Schema(description = "Remaining minutes (dailyLimitMinutes - actualMinutes; negative if exceeded)")
    private int remainingMinutes;
}

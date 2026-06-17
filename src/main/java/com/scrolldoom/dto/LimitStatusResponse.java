package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Daily limit status for an app, including lockout state")
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

    @Schema(description = "Number of breaches before auto-lockout", example = "3")
    @Builder.Default
    private int breachThreshold = 3;

    @Schema(description = "Whether the app is currently locked")
    private boolean blocked;

    @Schema(description = "Who locked the app: 'partner' or 'auto'")
    private String blockedBy;

    @Schema(description = "When the auto-lockout expires (null for partner locks)")
    private Date lockedUntil;

    @Schema(description = "How many more breaches are allowed before auto-lockout (0 if already locked)")
    @Builder.Default
    private int breachesRemaining = 0;
}

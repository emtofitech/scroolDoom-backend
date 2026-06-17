package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "An app time limit for the current user")
public class AppLimitResponse {

    @Schema(description = "MongoDB ObjectId as hex string", example = "664a1b2c3d4e5f6a7b8c9d0e")
    private String id;

    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;

    @Schema(description = "Daily time limit in minutes", example = "30")
    private int dailyLimitMinutes;

    @Schema(description = "Number of breaches before auto-lockout", example = "3")
    @Builder.Default
    private int breachThreshold = 3;

    @Schema(description = "Last update timestamp")
    private Date updatedAt;
}

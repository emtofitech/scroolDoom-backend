package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for user login/session creation")
public class LoginRequest {

    @Schema(description = "FCM device token for push notifications", example = "fcm-device-token")
    private String fcmToken;

    @Schema(description = "Device information (OS, app version)", example = "Android 14 / DoomScroll 1.0")
    private String deviceInfo;

    @Schema(description = "Remember me — extends session to 30 days", example = "false")
    private boolean rememberMe;
}

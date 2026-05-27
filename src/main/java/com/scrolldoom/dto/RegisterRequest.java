package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for user registration")
public class RegisterRequest {

    @NotBlank
    @Schema(description = "Firebase UID from the authenticated user", example = "abc123def456")
    private String firebaseUid;

    @NotBlank
    @Schema(description = "Display name shown to partner", example = "John Doe")
    private String displayName;

    @NotBlank
    @Email
    @Schema(description = "User email address", example = "john@example.com")
    private String email;

    @Schema(description = "FCM device token for push notifications (optional on register)", example = "fcm-device-token", nullable = true)
    private String fcmToken;

    @Schema(description = "Password for email/password authentication (optional if using Firebase)", example = "securePassword123", nullable = true)
    private String password;
}

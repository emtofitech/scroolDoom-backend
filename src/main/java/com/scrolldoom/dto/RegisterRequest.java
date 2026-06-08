package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    @Schema(description = "Password for email/password authentication (optional if using Firebase)", example = "securePassword123", nullable = true)
    private String password;
}

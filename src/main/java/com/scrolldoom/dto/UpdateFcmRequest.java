package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body to update the FCM device token")
public class UpdateFcmRequest {

    @NotBlank
    @Schema(description = "New FCM device token", example = "new-fcm-device-token")
    private String fcmToken;
}

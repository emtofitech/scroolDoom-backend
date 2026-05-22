package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Public user profile")
public class UserResponse {

    @Schema(description = "MongoDB ObjectId as hex string", example = "664a1b2c3d4e5f6a7b8c9d0e")
    private String id;

    @Schema(description = "Display name", example = "John Doe")
    private String displayName;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg", nullable = true)
    private String avatarUrl;

    @Schema(description = "Firebase UID", example = "abc123def456")
    private String firebaseUid;

    @Schema(description = "Account creation timestamp")
    private Date createdAt;
}

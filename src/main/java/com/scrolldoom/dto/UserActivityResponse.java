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
@Schema(description = "User activity status response")
public class UserActivityResponse {

    @Schema(description = "Activity ID")
    private String id;

    @Schema(description = "Whether the user is currently online")
    private boolean online;

    @Schema(description = "Last viewed screen", example = "HomeScreen")
    private String lastScreen;

    @Schema(description = "Last used feature", example = "feed")
    private String lastFeature;

    @Schema(description = "Last seen timestamp")
    private Date lastSeen;

    @Schema(description = "Last app open timestamp")
    private Date lastAppOpen;

    @Schema(description = "Last app close timestamp")
    private Date lastAppClose;

    @Schema(description = "Total session time in milliseconds")
    private long totalSessionMs;

    @Schema(description = "Number of sessions")
    private int sessionCount;

    @Schema(description = "Creation timestamp")
    private Date createdAt;

    @Schema(description = "Last update timestamp")
    private Date updatedAt;
}

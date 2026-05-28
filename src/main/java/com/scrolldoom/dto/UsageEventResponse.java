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
@Schema(description = "App usage event response")
public class UsageEventResponse {

    @Schema(description = "Event ID")
    private String id;

    @Schema(description = "Event type", example = "screen_view")
    private String eventType;

    @Schema(description = "Screen name", example = "HomeScreen")
    private String screenName;

    @Schema(description = "Feature name", example = "feed")
    private String featureName;

    @Schema(description = "Duration in milliseconds", example = "15000")
    private long durationMs;

    @Schema(description = "Device info", example = "curl-test")
    private String deviceInfo;

    @Schema(description = "App version", example = "1.0.0")
    private String appVersion;

    @Schema(description = "Timestamp when the event was recorded")
    private Date timestamp;
}

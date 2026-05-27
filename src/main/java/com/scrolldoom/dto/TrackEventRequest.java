package com.scrolldoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {
    private String eventType;
    private String screenName;
    private String featureName;
    private long durationMs;
    private String deviceInfo;
    private String appVersion;
}

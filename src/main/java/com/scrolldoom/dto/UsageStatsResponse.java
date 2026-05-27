package com.scrolldoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsResponse {
    private boolean online;
    private Date lastSeen;
    private String lastScreen;
    private int sessionCount;
    private long totalSessionMs;
    private long totalScreenViews;
    private long totalNotifications;
    private long notificationsOpened;
}

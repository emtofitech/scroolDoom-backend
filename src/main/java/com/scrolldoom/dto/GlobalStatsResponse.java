package com.scrolldoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalStatsResponse {
    private long onlineUsers;
    private long activeToday;
    private long activeThisWeek;
    private long totalEvents;
    private long eventsToday;
    private long totalNotifications;
    private long notificationsDelivered;
    private long notificationsOpened;
    private double deliveryRate;
    private double openRate;
}

package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "The user's streak information")
public class StreakResponse {

    @Schema(description = "Current consecutive clean-day streak", example = "5")
    private int currentStreak;

    @Schema(description = "Longest streak ever achieved", example = "12")
    private int longestStreak;

    @Schema(description = "Most recent day the user had no breaches", example = "2025-01-01", nullable = true)
    private LocalDate lastSuccessDate;

    @Schema(description = "Last calculation timestamp")
    private Date updatedAt;
}

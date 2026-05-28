package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to report a streak breach")
public class StreakBreachRequest {

    @NotBlank(message = "Streak name is required")
    @Schema(description = "Name of the streak that was broken", example = "No Instagram Before Noon", required = true)
    private String streakName;

    @Min(value = 1, message = "Missed days must be at least 1")
    @Schema(description = "Number of consecutive days missed", example = "3", required = true)
    private int missedDays;
}

package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Request body to update the daily minute limit for an app")
public class UpdateLimitRequest {

    @Min(1)
    @Max(1440)
    @Schema(description = "Updated daily time limit in minutes (1–1440)", example = "45")
    private int dailyLimitMinutes;

    @Schema(description = "Number of breaches before auto-lockout", example = "3")
    private Integer breachThreshold;
}

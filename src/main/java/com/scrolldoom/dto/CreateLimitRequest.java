package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body to create an app limit")
public class CreateLimitRequest {

    @NotBlank
    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @NotBlank
    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;

    @Min(1)
    @Max(1440)
    @Schema(description = "Daily time limit in minutes (1–1440)", example = "30")
    private int dailyLimitMinutes;
}

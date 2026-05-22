package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body to report an app usage breach")
public class CreateBreachRequest {

    @NotBlank
    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @NotBlank
    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;

    @Min(1)
    @Schema(description = "Daily limit in minutes that was set for this app", example = "30")
    private int limitMinutes;

    @Min(1)
    @Schema(description = "Actual minutes used that triggered the breach", example = "45")
    private int actualMinutes;
}

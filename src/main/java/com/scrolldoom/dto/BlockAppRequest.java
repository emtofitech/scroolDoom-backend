package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to lock an app for a user")
public class BlockAppRequest {

    @NotBlank
    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android")
    private String packageName;

    @NotBlank
    @Schema(description = "Human-readable app label", example = "Instagram")
    private String appLabel;
}

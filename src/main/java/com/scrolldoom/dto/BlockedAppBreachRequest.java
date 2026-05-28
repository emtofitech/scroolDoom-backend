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
@Schema(description = "Request to report a blocked app breach")
public class BlockedAppBreachRequest {

    @NotBlank(message = "Package name is required")
    @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android", required = true)
    private String packageName;

    @NotBlank(message = "App label is required")
    @Schema(description = "Human-readable app label", example = "Instagram", required = true)
    private String appLabel;
}

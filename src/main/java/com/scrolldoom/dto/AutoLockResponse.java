package com.scrolldoom.dto;

import com.scrolldoom.model.BlockedApp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Response after attempting to auto-lock an app for exceeding threshold")
public class AutoLockResponse {

    @Schema(description = "Whether the app was locked or is already locked")
    private boolean locked;

    @Schema(description = "The blocked app record (present when locked or already blocked)")
    private BlockedApp blockedApp;

    @Schema(description = "Human-readable message about the result")
    private String message;
}

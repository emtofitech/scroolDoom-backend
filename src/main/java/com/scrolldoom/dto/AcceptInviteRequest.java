package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body to accept a partnership invite")
public class AcceptInviteRequest {

    @NotBlank
    @Schema(description = "6-character invite code", example = "XK4M9P")
    private String inviteCode;
}

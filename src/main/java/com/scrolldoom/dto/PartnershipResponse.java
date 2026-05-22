package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "A partnership between two users")
public class PartnershipResponse {

    @Schema(description = "MongoDB ObjectId as hex string", example = "664a1b2c3d4e5f6a7b8c9d0e")
    private String id;

    @Schema(description = "Partnership status", allowableValues = {"pending", "active", "dissolved"}, example = "active")
    private String status;

    @Schema(description = "6-character invite code used to form the partnership", example = "XK4M9P")
    private String inviteCode;

    @Schema(description = "Invite creation timestamp")
    private Date createdAt;

    @Schema(description = "Timestamp when the invite was accepted (null if pending)", nullable = true)
    private Date acceptedAt;

    @Schema(description = "Partner user profile (null for pending invites)")
    private UserResponse partner;
}

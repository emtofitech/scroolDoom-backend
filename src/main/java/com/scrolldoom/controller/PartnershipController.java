package com.scrolldoom.controller;

import com.scrolldoom.dto.AcceptInviteRequest;
import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.PartnershipResponse;
import com.scrolldoom.service.PartnershipService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partnerships")
@Tag(name = "Partnerships", description = "Partner invite and management system")
@SecurityRequirement(name = "bearerAuth")
public class PartnershipController {

    private final PartnershipService partnershipService;
    private final UserService userService;

    public PartnershipController(PartnershipService partnershipService,
                                 UserService userService) {
        this.partnershipService = partnershipService;
        this.userService = userService;
    }

    @PostMapping("/invite")
    @Operation(summary = "Generate an invite code",
            description = "Generates a 6-character invite code valid for 24 hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invite code generated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "User already has an active partnership or pending invite")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<PartnershipResponse>> generateInvite() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.scrolldoom.dto.ApiEnvelope.ok(partnershipService.generateInvite(userId)));
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invite",
            description = "Accepts a pending invite code to form an active partnership.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partnership activated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Invite code not found"),
            @ApiResponse(responseCode = "409", description = "Invite expired / own invite / already has partnership")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<PartnershipResponse>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(
                partnershipService.acceptInvite(userId, req.getInviteCode())));
    }

    @GetMapping("/me")
    @Operation(summary = "Get active partnership",
            description = "Returns the current user's active partnership along with partner's profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active partnership retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership found")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<PartnershipResponse>> getActivePartnership() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(
                partnershipService.getActivePartnership(userId)));
    }

    @DeleteMapping("/invite")
    @Operation(summary = "Delete own pending invite",
            description = "Deletes the current user's pending invite code if it has not been accepted yet.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pending invite deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No pending invite found")
    })
    public ResponseEntity<Void> deleteOwnPendingInvite() {
        ObjectId userId = userService.getCurrentUserId();
        partnershipService.deleteOwnPendingInvite(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dissolve a partnership",
            description = "Soft-deletes a partnership by setting its status to 'dissolved'.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Partnership dissolved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "User is not a participant"),
            @ApiResponse(responseCode = "404", description = "Partnership not found")
    })
    public ResponseEntity<Void> dissolvePartnership(@PathVariable String id) {
        ObjectId userId = userService.getCurrentUserId();
        partnershipService.dissolvePartnership(userId, id);
        return ResponseEntity.noContent().build();
    }
}

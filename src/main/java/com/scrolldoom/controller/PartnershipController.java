package com.scrolldoom.controller;

import com.scrolldoom.dto.AcceptInviteRequest;
import com.scrolldoom.dto.PartnershipResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.UserRepository;
import com.scrolldoom.service.PartnershipService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partnerships")
@Tag(name = "Partnerships", description = "Partner invite and management system")
@SecurityRequirement(name = "bearerAuth")
public class PartnershipController {

    private final PartnershipService partnershipService;
    private final UserRepository userRepository;

    public PartnershipController(PartnershipService partnershipService,
                                 UserRepository userRepository) {
        this.partnershipService = partnershipService;
        this.userRepository = userRepository;
    }

    @PostMapping("/invite")
    @Operation(summary = "Generate an invite code",
            description = "Generates a 6-character uppercase alphanumeric invite code valid for 24 hours. "
                    + "Throws 409 if the user already has an active partnership or an unexpired pending invite. "
                    + "Any previously expired pending invite is automatically deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invite code generated",
                    content = @Content(schema = @Schema(implementation = PartnershipResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "status": "pending",
                                      "inviteCode": "XK4M9P",
                                      "createdAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "User already has an active partnership or pending invite")
    })
    public ResponseEntity<PartnershipResponse> generateInvite() {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnershipService.generateInvite(userId));
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invite",
            description = "Accepts a pending invite code to form an active partnership. "
                    + "Validates that the invite is pending, not expired, and not the user's own. "
                    + "The receiver must not already have an active partnership.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partnership activated — partner profile included",
                    content = @Content(schema = @Schema(implementation = PartnershipResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "status": "active",
                                      "inviteCode": "XK4M9P",
                                      "createdAt": "2025-01-01T00:00:00.000+00:00",
                                      "acceptedAt": "2025-01-01T01:00:00.000+00:00",
                                      "partner": {
                                        "id": "664a1b2c3d4e5f6a7b8c9d0f",
                                        "displayName": "Jane Doe",
                                        "email": "jane@example.com",
                                        "avatarUrl": null,
                                        "firebaseUid": "xyz789",
                                        "createdAt": "2025-01-01T00:00:00.000+00:00"
                                      }
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Invite code not found"),
            @ApiResponse(responseCode = "409", description = "Invite expired / own invite / already has partnership")
    })
    public ResponseEntity<PartnershipResponse> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest req) {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(partnershipService.acceptInvite(userId, req.getInviteCode()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get active partnership",
            description = "Returns the current user's active partnership (if any) along with their partner's profile. "
                    + "Throws 404 if no active partnership exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active partnership retrieved",
                    content = @Content(schema = @Schema(implementation = PartnershipResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership found")
    })
    public ResponseEntity<PartnershipResponse> getActivePartnership() {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(partnershipService.getActivePartnership(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dissolve a partnership",
            description = "Soft-deletes a partnership by setting its status to 'dissolved'. "
                    + "The partnership document is kept for history. "
                    + "Throws 403 if the current user is not a participant.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Partnership dissolved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "User is not a participant"),
            @ApiResponse(responseCode = "404", description = "Partnership not found")
    })
    public ResponseEntity<Void> dissolvePartnership(@PathVariable String id) {
        ObjectId userId = resolveCurrentUserId();
        partnershipService.dissolvePartnership(userId, id);
        return ResponseEntity.noContent().build();
    }

    private ObjectId resolveCurrentUserId() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}

package com.scrolldoom.controller;

import com.scrolldoom.dto.StreakResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.PartnershipRepository;
import com.scrolldoom.repository.UserRepository;
import com.scrolldoom.service.StreakService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/streaks")
@Tag(name = "Streaks", description = "Streak tracking for consecutive clean days")
@SecurityRequirement(name = "bearerAuth")
public class StreakController {

    private final StreakService streakService;
    private final UserRepository userRepository;
    private final PartnershipRepository partnershipRepository;

    public StreakController(StreakService streakService,
                            UserRepository userRepository,
                            PartnershipRepository partnershipRepository) {
        this.streakService = streakService;
        this.userRepository = userRepository;
        this.partnershipRepository = partnershipRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my streak",
            description = "Returns the current user's streak information. "
                    + "Streaks are recalculated lazily — only when this endpoint is called. "
                    + "A clean day means no breach events were recorded for yesterday. "
                    + "If already calculated today, returns cached values without recomputing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Streak data retrieved",
                    content = @Content(schema = @Schema(implementation = StreakResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "currentStreak": 5,
                                      "longestStreak": 12,
                                      "lastSuccessDate": "2025-01-01",
                                      "updatedAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<StreakResponse> getMyStreak() {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(streakService.getOrCalculateStreak(userId));
    }

    @GetMapping("/partner")
    @Operation(summary = "Get partner's streak",
            description = "Returns the current user's partner streak information. "
                    + "Throws 404 if the user has no active partnership. "
                    + "The partner's streak is recalculated lazily on each call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partner's streak data retrieved",
                    content = @Content(schema = @Schema(implementation = StreakResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership")
    })
    public ResponseEntity<StreakResponse> getPartnerStreak() {
        ObjectId userId = resolveCurrentUserId();
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership"));
        ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();
        return ResponseEntity.ok(streakService.getOrCalculateStreak(partnerId));
    }

    private ObjectId resolveCurrentUserId() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}

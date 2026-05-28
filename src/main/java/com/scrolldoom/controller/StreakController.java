package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.StreakResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.repository.PartnershipRepository;
import com.scrolldoom.service.StreakService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
    private final UserService userService;
    private final PartnershipRepository partnershipRepository;

    public StreakController(StreakService streakService,
                            UserService userService,
                            PartnershipRepository partnershipRepository) {
        this.streakService = streakService;
        this.userService = userService;
        this.partnershipRepository = partnershipRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my streak",
            description = "Returns the current user's streak information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Streak data retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<StreakResponse>> getMyStreak() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(streakService.getOrCalculateStreak(userId)));
    }

    @GetMapping("/partner")
    @Operation(summary = "Get partner's streak",
            description = "Returns the current user's partner streak information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partner's streak data retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<StreakResponse>> getPartnerStreak() {
        ObjectId userId = userService.getCurrentUserId();
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership"));
        ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(streakService.getOrCalculateStreak(partnerId)));
    }
}

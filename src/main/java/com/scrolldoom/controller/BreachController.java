package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.BlockedAppBreachRequest;
import com.scrolldoom.dto.BreachEventResponse;
import com.scrolldoom.dto.CreateBreachRequest;
import com.scrolldoom.dto.StreakBreachRequest;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.service.BreachService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/breaches")
@Tag(name = "Breaches", description = "Breach event reporting and partner notifications")
@SecurityRequirement(name = "bearerAuth")
public class BreachController {

    private final BreachService breachService;
    private final UserService userService;

    public BreachController(BreachService breachService, UserService userService) {
        this.breachService = breachService;
        this.userService = userService;
    }

    @PostMapping("/screen-time")
    @Operation(summary = "Report a screen time breach",
            description = "Records that the user exceeded their daily limit for a specific app.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<BreachEventResponse>> reportScreenTimeBreach(
            @Valid @RequestBody CreateBreachRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.scrolldoom.dto.ApiEnvelope.ok(breachService.reportBreach(userId, req)));
    }

    @PostMapping("/streak")
    @Operation(summary = "Report a streak broken",
            description = "Records that the user broke a streak.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<BreachEventResponse>> reportStreakBroken(
            @Valid @RequestBody StreakBreachRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        BreachEvent breach = breachService.reportStreakBroken(userId, req.getStreakName(), req.getMissedDays());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.scrolldoom.dto.ApiEnvelope.ok(mapToResponse(breach)));
    }

    @PostMapping("/blocked-app")
    @Operation(summary = "Report a blocked app opened",
            description = "Records that the user attempted to open a blocked app.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<BreachEventResponse>> reportBlockedAppOpened(
            @Valid @RequestBody BlockedAppBreachRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        BreachEvent breach = breachService.reportBlockedAppOpened(userId, req.getPackageName(), req.getAppLabel());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.scrolldoom.dto.ApiEnvelope.ok(mapToResponse(breach)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my breaches",
            description = "Returns all breach events for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of breach events"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<List<BreachEventResponse>>> getMyBreaches() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(breachService.getMyBreaches(userId)));
    }

    @GetMapping("/me/type/{breachType}")
    @Operation(summary = "Get my breaches by type",
            description = "Returns breach events filtered by type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of breach events"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<List<BreachEventResponse>>> getMyBreachesByType(
            @PathVariable String breachType) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(
                breachService.getMyBreachesByType(userId, breachType)));
    }

    @PatchMapping("/{breachId}/acknowledge")
    @Operation(summary = "Acknowledge a breach",
            description = "Marks a breach event as acknowledged by the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Breach acknowledged"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Breach not found")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<BreachEventResponse>> acknowledgeBreach(
            @PathVariable String breachId) {
        ObjectId userId = userService.getCurrentUserId();
        BreachEvent breach = breachService.acknowledgeBreach(new ObjectId(breachId), userId);
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(mapToResponse(breach)));
    }

    @GetMapping("/partner")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<Page<BreachEventResponse>>> getPartnerBreaches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean acknowledged) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(
                breachService.getPartnerBreaches(userId, acknowledged, PageRequest.of(page, size))));
    }

    @GetMapping("/partner/type/{breachType}")
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<Page<BreachEventResponse>>> getPartnerBreachesByType(
            @PathVariable String breachType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean acknowledged) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(
                breachService.getPartnerBreachesByType(userId, breachType, acknowledged, PageRequest.of(page, size))));
    }

    private BreachEventResponse mapToResponse(BreachEvent breach) {
        return BreachEventResponse.builder()
                .id(breach.getId().toHexString())
                .breachType(breach.getBreachType())
                .packageName(breach.getPackageName())
                .appLabel(breach.getAppLabel())
                .limitMinutes(breach.getLimitMinutes())
                .actualMinutes(breach.getActualMinutes())
                .streakName(breach.getStreakName())
                .missedDays(breach.getMissedDays())
                .severity(breach.getSeverity())
                .partnerNotified(breach.isPartnerNotified())
                .acknowledged(breach.isAcknowledged())
                .breachedAt(breach.getBreachedAt())
                .build();
    }
}

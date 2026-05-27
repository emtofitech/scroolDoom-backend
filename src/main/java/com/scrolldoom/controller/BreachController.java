package com.scrolldoom.controller;

import com.scrolldoom.dto.BreachEventResponse;
import com.scrolldoom.dto.CreateBreachRequest;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.UserRepository;
import com.scrolldoom.service.BreachService;
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
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/breaches")
@Tag(name = "Breaches", description = "Breach event reporting and partner notifications")
@SecurityRequirement(name = "bearerAuth")
public class BreachController {

    private final BreachService breachService;
    private final UserRepository userRepository;

    public BreachController(BreachService breachService, UserRepository userRepository) {
        this.breachService = breachService;
        this.userRepository = userRepository;
    }

    @PostMapping("/screen-time")
    @Operation(summary = "Report a screen time breach",
            description = "Records that the user exceeded their daily limit for a specific app. "
                    + "Duplicate breaches for the same app on the same calendar day are silently ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded (or existing breach returned)",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<BreachEventResponse> reportScreenTimeBreach(
            @Valid @RequestBody CreateBreachRequest req) {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(breachService.reportBreach(userId, req));
    }

    @PostMapping("/streak")
    @Operation(summary = "Report a streak broken",
            description = "Records that the user broke a streak. "
                    + "Duplicate breaches for the same streak on the same calendar day are silently ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded (or existing breach returned)",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<BreachEventResponse> reportStreakBroken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Streak breach details",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = StreakBreachRequest.class),
                            examples = @ExampleObject("""
                                    {
                                      "streakName": "No Instagram Before Noon",
                                      "missedDays": 3
                                    }""")))
            @Valid @RequestBody StreakBreachRequest req) {
        ObjectId userId = resolveCurrentUserId();
        BreachEvent breach = breachService.reportStreakBroken(userId, req.getStreakName(), req.getMissedDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(breach));
    }

    @PostMapping("/blocked-app")
    @Operation(summary = "Report a blocked app opened",
            description = "Records that the user attempted to open a blocked app. "
                    + "Duplicate breaches for the same app on the same calendar day are silently ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded (or existing breach returned)",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<BreachEventResponse> reportBlockedAppOpened(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Blocked app breach details",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BlockedAppBreachRequest.class),
                            examples = @ExampleObject("""
                                    {
                                      "packageName": "com.instagram.android",
                                      "appLabel": "Instagram"
                                    }""")))
            @Valid @RequestBody BlockedAppBreachRequest req) {
        ObjectId userId = resolveCurrentUserId();
        BreachEvent breach = breachService.reportBlockedAppOpened(userId, req.getPackageName(), req.getAppLabel());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(breach));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my breaches",
            description = "Returns all breach events for the current user, ordered by most recent first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of breach events",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<List<BreachEventResponse>> getMyBreaches() {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(breachService.getMyBreaches(userId));
    }

    @GetMapping("/me/type/{breachType}")
    @Operation(summary = "Get my breaches by type",
            description = "Returns breach events filtered by type (SCREEN_TIME_EXCEEDED, STREAK_BROKEN, BLOCKED_APP_OPENED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of breach events",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<List<BreachEventResponse>> getMyBreachesByType(
            @PathVariable String breachType) {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(breachService.getMyBreachesByType(userId, breachType));
    }

    @PatchMapping("/{breachId}/acknowledge")
    @Operation(summary = "Acknowledge a breach",
            description = "Marks a breach event as acknowledged by the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Breach acknowledged",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Breach not found")
    })
    public ResponseEntity<BreachEventResponse> acknowledgeBreach(@PathVariable String breachId) {
        ObjectId userId = resolveCurrentUserId();
        BreachEvent breach = breachService.acknowledgeBreach(new ObjectId(breachId), userId);
        return ResponseEntity.ok(mapToResponse(breach));
    }

    @GetMapping("/partner")
    @Operation(summary = "Get partner's breaches",
            description = "Returns all breach events for the current user's partner. "
                    + "Throws 404 if the user has no active partnership.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of partner's breach events",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership")
    })
    public ResponseEntity<List<BreachEventResponse>> getPartnerBreaches() {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.ok(breachService.getPartnerBreaches(userId));
    }

    private ObjectId resolveCurrentUserId() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
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

    @Data
    @Schema(description = "Request to report a streak breach")
    public static class StreakBreachRequest {
        @Schema(description = "Name of the streak that was broken", example = "No Instagram Before Noon", required = true)
        @jakarta.validation.constraints.NotBlank
        private String streakName;

        @Schema(description = "Number of consecutive days missed", example = "3", required = true)
        @jakarta.validation.constraints.Min(1)
        private int missedDays;
    }

    @Data
    @Schema(description = "Request to report a blocked app breach")
    public static class BlockedAppBreachRequest {
        @Schema(description = "Android package name or iOS bundle identifier", example = "com.instagram.android", required = true)
        @jakarta.validation.constraints.NotBlank
        private String packageName;

        @Schema(description = "Human-readable app label", example = "Instagram", required = true)
        @jakarta.validation.constraints.NotBlank
        private String appLabel;
    }
}

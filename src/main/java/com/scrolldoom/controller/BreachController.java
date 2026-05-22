package com.scrolldoom.controller;

import com.scrolldoom.dto.BreachEventResponse;
import com.scrolldoom.dto.CreateBreachRequest;
import com.scrolldoom.exception.ResourceNotFoundException;
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
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    @Operation(summary = "Report a breach",
            description = "Records that the user exceeded their daily limit for a specific app. "
                    + "If the user has an active partnership, an FCM push notification is sent to the partner. "
                    + "Duplicate breaches for the same app on the same calendar day are silently ignored. "
                    + "FCM failures are logged but never fail the request.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Breach recorded (or existing breach returned)",
                    content = @Content(schema = @Schema(implementation = BreachEventResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "packageName": "com.instagram.android",
                                      "appLabel": "Instagram",
                                      "limitMinutes": 30,
                                      "actualMinutes": 45,
                                      "partnerNotified": true,
                                      "breachedAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<BreachEventResponse> reportBreach(
            @Valid @RequestBody CreateBreachRequest req) {
        ObjectId userId = resolveCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(breachService.reportBreach(userId, req));
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
}

package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.AppLimitResponse;
import com.scrolldoom.dto.BlockAppRequest;
import com.scrolldoom.dto.CreateLimitRequest;
import com.scrolldoom.dto.LimitStatusResponse;
import com.scrolldoom.dto.UpdateLimitRequest;
import com.scrolldoom.model.BlockedApp;
import com.scrolldoom.service.AppLimitService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/limits")
@Tag(name = "App Limits", description = "Manage daily time limits for specific apps")
@SecurityRequirement(name = "bearerAuth")
public class LimitController {

    private final AppLimitService appLimitService;
    private final UserService userService;

    public LimitController(AppLimitService appLimitService, UserService userService) {
        this.appLimitService = appLimitService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get all app limits",
            description = "Returns all daily time limits configured by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of app limits"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<List<AppLimitResponse>>> getAllLimits() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.getLimits(userId)));
    }

    @GetMapping("/status")
    @Operation(summary = "Get limit statuses",
            description = "Returns whether each app limit has been exceeded today, including lockout state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of limit statuses"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<List<LimitStatusResponse>>> getLimitStatuses() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.getLimitStatuses(userId)));
    }

    @GetMapping("/blocked")
    @Operation(summary = "List blocked apps",
            description = "Returns all currently locked apps for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of blocked apps"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<List<BlockedApp>>> listBlockedApps() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.listBlockedApps(userId)));
    }

    @PostMapping("/blocked")
    @Operation(summary = "Lock an app for your partner",
            description = "Creates a block for your partner's app. The partner will be notified via push.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "App locked for partner"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "No active partnership found")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<BlockedApp>> lockPartnerApp(
            @Valid @RequestBody BlockAppRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.lockPartnerApp(userId, req)));
    }

    @DeleteMapping("/blocked/{packageName}")
    @Operation(summary = "Unlock an app",
            description = "Removes a lock from an app for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "App unlocked"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<Void> unlockApp(@PathVariable String packageName) {
        ObjectId userId = userService.getCurrentUserId();
        appLimitService.unlockApp(userId, packageName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Create an app limit",
            description = "Sets a daily time limit for a specific app.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "App limit created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "Limit already exists for this app")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<AppLimitResponse>> createLimit(
            @Valid @RequestBody CreateLimitRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.createLimit(userId, req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an app limit",
            description = "Updates the daily minute limit for an existing app limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "App limit updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Limit belongs to another user"),
            @ApiResponse(responseCode = "404", description = "App limit not found")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<AppLimitResponse>> updateLimit(
            @PathVariable String id,
            @Valid @RequestBody UpdateLimitRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(appLimitService.updateLimit(userId, id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an app limit",
            description = "Removes a daily time limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "App limit deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Limit belongs to another user"),
            @ApiResponse(responseCode = "404", description = "App limit not found")
    })
    public ResponseEntity<Void> deleteLimit(@PathVariable String id) {
        ObjectId userId = userService.getCurrentUserId();
        appLimitService.deleteLimit(userId, id);
        return ResponseEntity.noContent().build();
    }
}

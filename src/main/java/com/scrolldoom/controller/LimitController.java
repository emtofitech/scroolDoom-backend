package com.scrolldoom.controller;

import com.scrolldoom.dto.AppLimitResponse;
import com.scrolldoom.dto.CreateLimitRequest;
import com.scrolldoom.dto.UpdateLimitRequest;
import com.scrolldoom.service.AppLimitService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @ApiResponse(responseCode = "200", description = "List of app limits",
                    content = @Content(schema = @Schema(implementation = AppLimitResponse.class),
                            examples = @ExampleObject("""
                                    [{
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "packageName": "com.instagram.android",
                                      "appLabel": "Instagram",
                                      "dailyLimitMinutes": 30,
                                      "updatedAt": "2025-01-01T00:00:00.000+00:00"
                                    }]"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<List<AppLimitResponse>> getAllLimits() {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(appLimitService.getLimits(userId));
    }

    @PostMapping
    @Operation(summary = "Create an app limit",
            description = "Sets a daily time limit for a specific app. "
                    + "Throws 409 if a limit for this packageName already exists for the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "App limit created",
                    content = @Content(schema = @Schema(implementation = AppLimitResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "packageName": "com.instagram.android",
                                      "appLabel": "Instagram",
                                      "dailyLimitMinutes": 30,
                                      "updatedAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "Limit already exists for this app")
    })
    public ResponseEntity<AppLimitResponse> createLimit(@Valid @RequestBody CreateLimitRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appLimitService.createLimit(userId, req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an app limit",
            description = "Updates the daily minute limit for an existing app limit. "
                    + "Throws 403 if the limit does not belong to the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "App limit updated",
                    content = @Content(schema = @Schema(implementation = AppLimitResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "packageName": "com.instagram.android",
                                      "appLabel": "Instagram",
                                      "dailyLimitMinutes": 45,
                                      "updatedAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Limit belongs to another user"),
            @ApiResponse(responseCode = "404", description = "App limit not found")
    })
    public ResponseEntity<AppLimitResponse> updateLimit(@PathVariable String id,
                                                        @Valid @RequestBody UpdateLimitRequest req) {
        ObjectId userId = userService.getCurrentUserId();
        return ResponseEntity.ok(appLimitService.updateLimit(userId, id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an app limit",
            description = "Removes a daily time limit. "
                    + "Throws 403 if the limit does not belong to the current user.")
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

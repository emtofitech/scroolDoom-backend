package com.scrolldoom.controller;

import com.scrolldoom.dto.UpdateFcmRequest;
import com.scrolldoom.dto.UserResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile",
            description = "Returns the authenticated user's profile. Updates lastActiveAt on every call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject("""
                                    {
                                      "id": "664a1b2c3d4e5f6a7b8c9d0e",
                                      "displayName": "John Doe",
                                      "email": "john@example.com",
                                      "avatarUrl": null,
                                      "firebaseUid": "abc123def456",
                                      "createdAt": "2025-01-01T00:00:00.000+00:00"
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getMe() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        return ResponseEntity.ok(userService.getCurrentUserProfile(firebaseUid));
    }

    @PatchMapping("/me/fcm")
    @Operation(summary = "Update FCM device token",
            description = "Overwrites the FCM push notification token for the current user. "
                    + "Should be called on every app launch.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — missing fcmToken"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> updateFcm(@Valid @RequestBody UpdateFcmRequest req) {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        userService.updateFcmToken(firebaseUid, req.getFcmToken());
        return ResponseEntity.noContent().build();
    }
}

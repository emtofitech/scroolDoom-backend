package com.scrolldoom.controller;

import com.scrolldoom.dto.ApiEnvelope;
import com.scrolldoom.dto.UpdateFcmRequest;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
            @ApiResponse(responseCode = "200", description = "User profile retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.scrolldoom.dto.ApiEnvelope<UserResponse>> getMe() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        return ResponseEntity.ok(com.scrolldoom.dto.ApiEnvelope.ok(userService.getCurrentUserProfile(firebaseUid)));
    }

    @PatchMapping("/me/fcm")
    @Operation(summary = "Update FCM device token",
            description = "Overwrites the FCM push notification token for the current user.")
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

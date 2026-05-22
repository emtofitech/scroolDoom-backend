package com.scrolldoom.controller;

import com.scrolldoom.dto.RegisterRequest;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Public authentication endpoints (no JWT required)")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Creates a new user account or returns the existing user if the "
                    + "firebaseUid is already registered. Idempotent — safe to call on every app launch.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created or existing user returned",
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
            @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(req));
    }
}

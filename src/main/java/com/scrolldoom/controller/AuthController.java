package com.scrolldoom.controller;

import com.scrolldoom.dto.LoginRequest;
import com.scrolldoom.dto.LoginWithPasswordRequest;
import com.scrolldoom.dto.LoginWithPasswordResponse;
import com.scrolldoom.dto.RefreshTokenRequest;
import com.scrolldoom.dto.RefreshTokenResponse;
import com.scrolldoom.dto.RegisterRequest;
import com.scrolldoom.dto.SessionResponse;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.service.SessionService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication and session management endpoints")
public class AuthController {

    private final UserService userService;
    private final SessionService sessionService;

    public AuthController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
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

    @PostMapping("/login")
    @Operation(summary = "Create a session",
            description = "Creates a new session for the authenticated user. "
                    + "The Firebase ID token must be provided in the Authorization header. "
                    + "Returns session details including session ID for future management.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created successfully",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing Firebase token")
    })
    public ResponseEntity<SessionResponse> login(@RequestBody(required = false) LoginRequest req,
                                                  HttpServletRequest request) {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        String deviceInfo = req != null ? req.getDeviceInfo() : request.getHeader("User-Agent");
        String fcmToken = req != null ? req.getFcmToken() : null;
        boolean rememberMe = req != null && req.isRememberMe();
        String ipAddress = request.getRemoteAddr();

        SessionResponse session = sessionService.createSession(firebaseUid, deviceInfo, ipAddress, fcmToken, rememberMe);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PostMapping("/login-with-password")
    @Operation(summary = "Login with email and password",
            description = "Authenticates a user with email and password, returns a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginWithPasswordResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<LoginWithPasswordResponse> loginWithPassword(@Valid @RequestBody LoginWithPasswordRequest req) {
        LoginWithPasswordResponse response = userService.loginWithPassword(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token",
            description = "Exchanges a valid refresh token for a new access token + new refresh token. "
                    + "The old refresh token is revoked (single-use).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest req) {
        RefreshTokenResponse response = userService.refreshAccessToken(req.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sliding-refresh")
    @Operation(summary = "Sliding window refresh",
            description = "Re-issues a new access token from a valid (non-expired) access token. "
                    + "No refresh token required — extends the session window without rotation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token expired or invalid")
    })
    public ResponseEntity<RefreshTokenResponse> slidingRefresh(@Valid @RequestBody RefreshTokenRequest req) {
        RefreshTokenResponse response = userService.slidingRefresh(req.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/firebase-refresh")
    @Operation(summary = "Refresh using Firebase ID token",
            description = "Verifies an expired/valid Firebase ID token and returns a new self-issued JWT. "
                    + "Useful for clients that already have a Firebase token and want a backend JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT issued from Firebase token",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid Firebase token")
    })
    public ResponseEntity<RefreshTokenResponse> firebaseRefresh(@Valid @RequestBody RefreshTokenRequest req) {
        RefreshTokenResponse response = userService.firebaseRefresh(req.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}

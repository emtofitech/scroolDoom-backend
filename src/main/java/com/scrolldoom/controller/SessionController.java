package com.scrolldoom.controller;

import com.scrolldoom.dto.SessionResponse;
import com.scrolldoom.service.SessionService;
import com.scrolldoom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Session management endpoints (requires JWT)")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "List active sessions",
            description = "Returns all active (non-expired, non-revoked) sessions for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active sessions")
    })
    public ResponseEntity<List<SessionResponse>> listSessions() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        return ResponseEntity.ok(sessionService.getUserSessions(firebaseUid));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a session",
            description = "Revokes a specific session by ID. The session will no longer be valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session revoked successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable String sessionId) {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        sessionService.revokeSession(firebaseUid, sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    @DeleteMapping
    @Operation(summary = "Revoke all sessions",
            description = "Revokes all active sessions for the current user except the current one.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All sessions revoked")
    })
    public ResponseEntity<Map<String, String>> revokeAllSessions() {
        String firebaseUid = UserService.getCurrentFirebaseUid();
        sessionService.revokeAllSessions(firebaseUid);
        return ResponseEntity.ok(Map.of("message", "All sessions revoked"));
    }
}

package com.scrolldoom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Service healthcheck")
public class HealthController {

    @GetMapping("/api/v1/health")
    @Operation(summary = "Healthcheck", description = "Returns 200 OK when the service is running.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is healthy",
                    content = @Content(examples = @ExampleObject("""
                            {"status": "UP"}""")))
    })
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

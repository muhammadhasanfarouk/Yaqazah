package com.yaqazah.session.controller;

import com.yaqazah.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Driving Sessions", description = "Endpoints for starting, pausing, resuming, and ending driving sessions.")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Operation(summary = "Start a new driving session", description = "Creates and starts a new session for the specified driver using their User ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session started successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires DRIVER role)"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @PostMapping("/start/{driverId}")
    public void createSession(@PathVariable UUID driverId) {
        sessionService.startSession(driverId);
    }

    @Operation(summary = "Pause an active session", description = "Pauses an ongoing driving session using the unique Session ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session paused successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires DRIVER role)"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/pause/{sessionId}")
    public void pauseSession(@PathVariable UUID sessionId) {
        sessionService.pauseSession(sessionId);
    }

    @Operation(summary = "Resume a paused session", description = "Resumes a previously paused driving session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session resumed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires DRIVER role)"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/resume/{sessionId}")
    public void resumeSession(@PathVariable UUID sessionId) {
        sessionService.resumeSession(sessionId);
    }

    @Operation(summary = "End a session", description = "Ends an active or paused driving session permanently.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session ended successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires DRIVER role)"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/end/{sessionId}")
    public void endSession(@PathVariable UUID sessionId) {
        sessionService.endSession(sessionId);
    }
}
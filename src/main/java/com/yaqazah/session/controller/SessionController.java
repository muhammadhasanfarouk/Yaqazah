package com.yaqazah.session.controller;

import com.yaqazah.session.dto.SessionUploadRequest;
import com.yaqazah.session.model.Session;
import com.yaqazah.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
@Tag(name = "Driving Sessions", description = "Endpoint for uploading a completed driving session with all its detection logs.")
@RequiredArgsConstructor
@NullMarked
public class SessionController {

    private final SessionService sessionService;

    @Operation(
            summary = "Upload a completed session",
            description = "Accepts a completed driving session and all its detection logs. " +
                    "Creates the session record and all log records in a single transaction."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('FLEET_DRIVER', 'INDEPENDENT_DRIVER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<String> uploadSession(
            @RequestBody SessionUploadRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails authenticatedUser
    ) {
        try {
            Session saved = sessionService.uploadSession(request, authenticatedUser.getUsername());
            return ResponseEntity.ok("Session uploaded successfully. ID: " + saved.getSessionId());
        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
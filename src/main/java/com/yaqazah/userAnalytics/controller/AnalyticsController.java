package com.yaqazah.userAnalytics.controller;

import com.yaqazah.adminAnalytics.dto.SessionDetailsResponseDto;
import com.yaqazah.adminAnalytics.dto.SessionsListResponseDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import com.yaqazah.userAnalytics.model.UserAnalyticsResponseDto;
import com.yaqazah.userAnalytics.service.NewUserAnalyticsService;
import com.yaqazah.userAnalytics.service.UserDriverAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
@RestController
@NullMarked
@RequestMapping("/api/user/analytics")
@Tag(name = "Analytics", description = "Endpoints for retrieving INDEPENDENT_DRIVER analytics and statistics.")
public class AnalyticsController {

    private final NewUserAnalyticsService newUserAnalyticsService;
    private final UserDriverAnalyticsService driverService;
    private final UserRepository userRepository;


    @Operation(summary = "List sessions", description = "Sessions and overview stats for the authenticated INDEPENDENT_DRIVER.")
    @PreAuthorize("hasAnyRole('INDEPENDENT_DRIVER', 'ADMIN')")
    @GetMapping("/sessions")
    public ResponseEntity<SessionsListResponseDto> getSessions(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        return executeForDriver(authentication, driverId ->
                newUserAnalyticsService.buildSessionsList(driverId, filter, from, to, page, size));
    }

    @Operation(summary = "Session details", description = "Detailed session data and detection logs for a single session.")
    @PreAuthorize("hasAnyRole('INDEPENDENT_DRIVER', 'ADMIN', 'COMPANY_ADMIN')")
    @GetMapping("/sessions/details/{sessionId}")
    public ResponseEntity<SessionDetailsResponseDto> getSessionDetails(
            @PathVariable UUID sessionId,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            UUID driverId = resolveDriverId(authentication);
            return newUserAnalyticsService.buildSessionDetails(driverId, sessionId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(summary = "User analytics", description = "High level analytics for the authenticated driver.")
    @PreAuthorize("hasAnyRole('INDEPENDENT_DRIVER', 'ADMIN')")
    @GetMapping("/getAnalytics")
    public ResponseEntity<UserAnalyticsResponseDto> getAnalytics(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @Parameter(hidden = true) Authentication authentication) {
        return executeForDriver(authentication, driverId ->
                driverService.buildAnalytics(driverId, filter, from, to));
    }

    private <T> ResponseEntity<T> executeForDriver(
            Authentication authentication,
            Function<UUID, T> action) {
        try {
            UUID driverId = resolveDriverId(authentication);
            return ResponseEntity.ok(action.apply(driverId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private UUID resolveDriverId(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getUserId();
    }
}
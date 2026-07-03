package com.yaqazah.adminAnalytics.controller;

import com.yaqazah.adminAnalytics.dto.DriverDetailResponseDto;
import com.yaqazah.adminAnalytics.dto.DriversListResponseDto;
import com.yaqazah.adminAnalytics.dto.SessionDetailsResponseDto;
import com.yaqazah.adminAnalytics.dto.SessionsListResponseDto;
import com.yaqazah.adminAnalytics.service.NewAdminDriversAnalyticsService;
import com.yaqazah.adminAnalytics.service.NewAdminSessionAnalyticsService;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/analytics")
@Tag(name = "Admin Analytics", description = "Company-scoped analytics for sessions and drivers")
@NullMarked
public class AdminAnalyticsController {

    private final UserRepository userRepository;
    private final NewAdminDriversAnalyticsService newAdminDriversAnalyticsService;
    private final NewAdminSessionAnalyticsService newAdminSessionAnalyticsService;

    public AdminAnalyticsController(
            UserRepository userRepository,
            NewAdminDriversAnalyticsService newAdminDriversAnalyticsService,
            NewAdminSessionAnalyticsService newAdminSessionAnalyticsService) {
        this.userRepository = userRepository;
        this.newAdminDriversAnalyticsService = newAdminDriversAnalyticsService;
        this.newAdminSessionAnalyticsService = newAdminSessionAnalyticsService;
    }

    @Operation(summary = "List sessions", description = "Sessions and overview stats for the authenticated company admin's fleet.")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @GetMapping("/sessions")
    public ResponseEntity<SessionsListResponseDto> getSessions(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "risk", required = false) String risk,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        return executeForCompany(authentication, companyId ->
                newAdminSessionAnalyticsService.buildSessionsList(companyId, filter, from, to, search, risk, sort, page, size));
    }

    @Operation(summary = "Session details", description = "Detailed session data and detection logs for a single session.")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @GetMapping("/sessions/details/{sessionId}")
    public ResponseEntity<SessionDetailsResponseDto> getSessionDetails(
            @PathVariable UUID sessionId,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            UUID companyId = resolveCompanyId(authentication);
            return newAdminSessionAnalyticsService.buildSessionDetails(companyId, sessionId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Operation(summary = "List drivers", description = "Fleet drivers with overview stats for the authenticated company.")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @GetMapping("/drivers")
    public ResponseEntity<DriversListResponseDto> getDrivers(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        return executeForCompany(authentication, companyId ->
                newAdminDriversAnalyticsService.buildDriversList(companyId, filter, from, to, search, sort, page, size));
    }

    @Operation(summary = "Driver analytics", description = "Detailed analytics for a single driver within a date range.")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @GetMapping("/driver")
    public ResponseEntity<DriverDetailResponseDto> getDriver(
            @RequestParam("email") String email,
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            UUID companyId = resolveCompanyId(authentication);
            User driver = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            return newAdminDriversAnalyticsService.buildDriverDetail(companyId, driver.getUserId(), filter, from, to)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private <T> ResponseEntity<T> executeForCompany(
            Authentication authentication,
            java.util.function.Function<UUID, T> action) {
        try {
            UUID companyId = resolveCompanyId(authentication);
            return ResponseEntity.ok(action.apply(companyId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private UUID resolveCompanyId(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getCompany() == null) {
            throw new IllegalStateException("User is not assigned to a company");
        }
        return user.getCompany().getCompanyId();
    }
}

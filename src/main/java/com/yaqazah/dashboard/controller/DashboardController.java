package com.yaqazah.dashboard.controller;

import com.yaqazah.dashboard.dto.DashboardResponseDto;
import com.yaqazah.dashboard.service.NewDashboardService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Dashboard", description = "Company admin analytics dashboard")
@NullMarked
public class DashboardController {

    private final UserRepository userRepository;
    private final NewDashboardService newDashboardService;

    public DashboardController(UserRepository userRepository, NewDashboardService newDashboardService) {
        this.userRepository = userRepository;
        this.newDashboardService = newDashboardService;
    }

    //new Code
    @Operation(summary = "Company dashboard", description = "Aggregated analytics for the authenticated company admin's fleet.")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @Parameter(hidden = true) Authentication authentication) {
        try {
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(principal.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));
            if (user.getCompany() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(newDashboardService.buildDashboard(
                    user.getCompany().getCompanyId(), filter, from, to));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

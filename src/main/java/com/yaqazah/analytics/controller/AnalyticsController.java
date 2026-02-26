package com.yaqazah.analytics.controller;

import com.yaqazah.analytics.service.CompanyAnalyticsService;
import com.yaqazah.analytics.service.DriverAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Endpoints for retrieving driver and company-wide analytics and statistics.")
public class AnalyticsController {

    // Explicitly ask for the Driver service
    @Autowired
    private DriverAnalyticsService driverService;

    // Explicitly ask for the Company service
    @Autowired
    private CompanyAnalyticsService companyService;

    @Operation(summary = "Get Driver Analytics", description = "Calculates and retrieves performance and detection analytics for a specific driver.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Driver analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires DRIVER or COMPANYADM role)"),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    @GetMapping("/driver/{driverId}")
    public void getDriverAnalytics(@PathVariable UUID driverId) {
        driverService.calculateAnalytics(driverId);
    }

    @Operation(summary = "Get Company Analytics", description = "Calculates and retrieves aggregated analytics data for an entire company fleet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Company analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires COMPANYADM or ADMIN role)"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/company/{companyId}")
    public void getCompanyAnalytics(@PathVariable UUID companyId) {
        companyService.calculateAnalytics(companyId);
    }
}
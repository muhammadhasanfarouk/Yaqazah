package com.yaqazah.analytics.controller;

import com.yaqazah.analytics.service.CompanyAnalyticsService;
import com.yaqazah.analytics.service.DriverAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    // Explicitly ask for the Driver service
    @Autowired
    private DriverAnalyticsService driverService;

    // Explicitly ask for the Company service
    @Autowired
    private CompanyAnalyticsService companyService;

    @GetMapping("/driver/{driverId}")
    public void getDriverAnalytics(@PathVariable UUID driverId) {
        driverService.calculateAnalytics(driverId);
    }

    @GetMapping("/company/{companyId}")
    public void getCompanyAnalytics(@PathVariable UUID companyId) {
        companyService.calculateAnalytics(companyId);
    }
}

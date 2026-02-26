package com.yaqazah.notification.controller;

import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.notification.model.AlertType;
import com.yaqazah.notification.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alert Configuration", description = "Endpoints for managing alert and notification settings for drivers.")
public class AlertController {

    @Autowired
    private AlertService service;

    @Operation(summary = "Update Alert Type", description = "Configures the specific type of alert (e.g., sound, vibration) triggered by a specific detection event (e.g., drowsiness).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert type configured successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid DetectionType or AlertType provided"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires specific role)")
    })
    @PutMapping("/config/{detectionType}")
    public void setAlertType(@PathVariable DetectionType detectionType, @RequestParam AlertType alertType) {
        service.updateAlertType(detectionType, alertType);
    }
}
package com.yaqazah.notification.controller;

import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.notification.model.AlertType;
import com.yaqazah.notification.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    @Autowired
    private AlertService service;

    @PutMapping("/config/{detectionType}")
    public void setAlertType(@PathVariable DetectionType detectionType, @RequestParam AlertType alertType) {
        service.updateAlertType(detectionType, alertType);
    }
}
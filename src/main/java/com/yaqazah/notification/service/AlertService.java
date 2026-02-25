package com.yaqazah.notification.service;

import com.yaqazah.notification.model.AlertConfig;
import com.yaqazah.notification.model.AlertType;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.notification.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    @Autowired
    private AlertRepository alertRepo;

    public void saveConfig(AlertConfig config) { alertRepo.save(config); }
    public void triggerAlert(DetectionType type) { /* Logic */ }
    public void updateAlertType(DetectionType detectionType, AlertType alertType) { /* Logic */ }
    public AlertConfig getConfig(DetectionType type) { return new AlertConfig(); }
}
package com.yaqazah.detection.service;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.detection.repository.DetectionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class DetectionService {
    @Autowired
    private DetectionLogRepository detectionLogRepo;

    private UUID sessionId;

    public void startDetection(UUID sessionId) { this.sessionId = sessionId; /* Logic */ }
    public void endDetection() { /* Logic */ }

    public DetectionLog log(String time, DetectionType type, Object frame) {
        // Build and save log
        return new DetectionLog();
    }
}
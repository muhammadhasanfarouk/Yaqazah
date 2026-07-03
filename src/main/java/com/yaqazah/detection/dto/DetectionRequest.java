package com.yaqazah.detection.dto;

//import com.yaqazah.detection.model.AlertType;

import com.yaqazah.detection.model.DetectionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data // This automatically adds Getters, Setters, and RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class DetectionRequest {
    private UUID sessionId;
    private String timestamp;
    private DetectionType type;

//    private RiskId riskId;
//    private AlertType alertType;

    // ADD THIS FIELD
    private String severity;

    private float valueDetected;
    private String photoBase64;
}
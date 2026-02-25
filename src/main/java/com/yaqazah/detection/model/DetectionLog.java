package com.yaqazah.detection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detection_event")
public class DetectionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID eventId;

    private UUID sessionId; // FK to Session
    private String timestamp;

    @Enumerated(EnumType.STRING)
    private DetectionType type;

    private String severity;
    private String snapshotProofUrl;
    private float valueDetected;

    // Getters and Setters
}
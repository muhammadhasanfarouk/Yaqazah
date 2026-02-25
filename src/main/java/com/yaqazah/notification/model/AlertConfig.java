package com.yaqazah.notification.model;

import com.yaqazah.detection.model.DetectionType;
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
@Table(name = "user_preference")
public class AlertConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID prefId;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private DetectionType detectionType;

    private String alertType; // Maps to STRING in ERD
}
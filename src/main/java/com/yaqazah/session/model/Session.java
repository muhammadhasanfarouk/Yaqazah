package com.yaqazah.session.model;

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
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    private UUID userId; // FK to User
    private String startTime;
    private String endTime;
    private float durationHours;
    private int totalAlerts;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    // Getters and Setters omitted for brevity
}
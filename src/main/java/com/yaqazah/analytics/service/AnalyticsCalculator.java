package com.yaqazah.analytics.service;

import com.yaqazah.detection.model.DetectionLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class AnalyticsCalculator {

    private List<DetectionLog> logs; //

    // Setter to load the logs before calculating
    public void setLogs(List<DetectionLog> logs) {
        this.logs = logs;
    }

    public void calculateStatistics() {
        // Master method to trigger all calculations
    }

    public int calculateTotalAlerts() {
        // Logic to count alerts in the logs
        return 0;
    }

    public double calculateAlertFrequency() {
        // Logic: alerts per hour
        return 0.0;
    }

    public String calculateSleepinessTrends() {
        // Logic to analyze patterns over time
        return "Stable";
    }

    public String suggestOptimalBreaks() {
        // Logic to recommend break times based on fatigue
        return "Take a break in 30 mins";
    }

    public double calculateTotalDrivingTime() {
        // Logic to sum up session durations
        return 0.0;
    }
}
package com.yaqazah.report.dto;

import java.util.UUID;

public record DriverSessionReportDto(
        UUID driverId,
        String driverFullName,
        UUID sessionId,
        String startDateTime,
        String endDateTime,
        Double durationHours,
        Integer totalAlerts,

        // Detection Info (Will be null if the session had no alerts)
        UUID eventId,
        String eventTimestamp,
        Integer alertId,
        Integer riskId,
        String title,
        String subtitle
) {
}
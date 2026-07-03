package com.yaqazah.report.service;

import com.yaqazah.adminAnalytics.dto.DriverSummaryDto;
import com.yaqazah.adminAnalytics.dto.SessionSummaryDto;
import com.yaqazah.adminAnalytics.service.NewAdminDriversAnalyticsService;
import com.yaqazah.adminAnalytics.service.NewAdminSessionAnalyticsService;
import com.yaqazah.dashboard.dto.DashboardResponseDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.service.NewDashboardService;
import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final NewDashboardService newDashboardService;
    private final NewAdminSessionAnalyticsService newAdminSessionAnalyticsService;
    private final NewAdminDriversAnalyticsService newAdminDriversAnalyticsService;

    @Cacheable(value = "reports:combined", key = "#companyId")
    public String generateCSVReport(UUID companyId) {
        List<DriverSessionReportDto> data = userRepository.findCombinedDriverDataByCompany(companyId);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Driver ID", "Driver Name", "Session ID", "Start Time", "End Time",
                        "Duration (Hrs)", "Total Alerts", "Event ID", "Event Time",
                        "Alert ID", "Risk ID", "Title", "Subtitle")
                .build();

        StringWriter writer = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            for (DriverSessionReportDto record : data) {
                String eventId = record.eventId() != null ? record.eventId().toString() : "N/A";
                String eventTime = record.eventTimestamp() != null ? record.eventTimestamp() : "N/A";
                String alertId = record.alertId() != null ? record.alertId().toString() : "N/A";
                String riskId = record.riskId() != null ? record.riskId().toString() : "N/A";
                String title = record.title() != null ? record.title() : "N/A";
                String subtitle = record.subtitle() != null ? record.subtitle() : "N/A";

                csvPrinter.printRecord(
                        record.driverId(),
                        record.driverFullName(),
                        record.sessionId(),
                        record.startDateTime(),
                        record.endDateTime(),
                        record.durationHours(),
                        record.totalAlerts(),
                        eventId,
                        eventTime,
                        alertId,
                        riskId,
                        title,
                        subtitle
                );
            }
            csvPrinter.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }

    @Cacheable(value = "reports:dashboard", key = "#companyId + ':' + #filter + ':' + (#from != null ? #from : '') + ':' + (#to != null ? #to : '')")
    public String generateDashboardCSV(UUID companyId, String filter, String from, String to) {
        DashboardResponseDto dashboardDto = newDashboardService.buildDashboard(companyId, filter, from, to);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Metric Group", "Metric Name", "Value", "Trend/Comparison")
                .build();

        StringWriter writer = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            // Overview Stats
            if (dashboardDto.getOverviewStats() != null) {
                for (OverviewStatDto stat : dashboardDto.getOverviewStats()) {
                    csvPrinter.printRecord("Overview", stat.getLabel(), stat.getValue(), stat.getDelta() != null ? stat.getDelta() : "N/A");
                }
            }

            // Risk Distribution
            if (dashboardDto.getRiskDistribution() != null) {
                for (RiskDistributionDto risk : dashboardDto.getRiskDistribution()) {
                    String label = switch (risk.getId()) {
                        case 0 -> "Low Risk";
                        case 1 -> "Medium Risk";
                        case 2 -> "High Risk";
                        default -> "Unknown Risk";
                    };
                    csvPrinter.printRecord("Risk Distribution", label, risk.getValue(), "N/A");
                }
            }

            // Pie/Alert Distribution
            if (dashboardDto.getPieDistribution() != null) {
                for (PieDistributionDto pie : dashboardDto.getPieDistribution()) {
                    String label = switch (pie.getId()) {
                        case 0 -> "Asleep";
                        case 1 -> "Drowsy";
                        case 2 -> "Distracted";
                        case 3 -> "Phone";
                        case 4 -> "LookingAway";
                        case 5 -> "EatingAndDrinking";
                        default -> "Unknown (" + pie.getId() + ")";
                    };
                    csvPrinter.printRecord("Alert Distribution", label, pie.getPercent() != null ? pie.getPercent() + "%" : "N/A", "N/A");
                }
            }

            csvPrinter.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Dashboard CSV report", e);
        }
    }

    @Cacheable(value = "reports:sessions", key = "#companyId + ':' + #filter + ':' + (#from != null ? #from : '') + ':' + (#to != null ? #to : '') + ':' + (#search != null ? #search : '') + ':' + (#risk != null ? #risk : '') + ':' + (#sort != null ? #sort : '')")
    public String generateSessionsCSV(UUID companyId, String filter, String from, String to, String search, String risk, String sort) {
        var sessionsResponse = newAdminSessionAnalyticsService.buildSessionsList(companyId, filter, from, to, search, risk, sort);
        List<SessionSummaryDto> sessions = sessionsResponse.getSessions();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Session ID", "Driver Name", "Driver ID", "Start Time", "End Time",
                        "Duration", "Safety Score", "Alert Count", "Risk Level")
                .build();

        StringWriter writer = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            if (sessions != null) {
                for (SessionSummaryDto session : sessions) {
                    String riskLevel = switch (session.getRiskId()) {
                        case 0 -> "Low Risk";
                        case 1 -> "Medium Risk";
                        case 2 -> "High Risk";
                        default -> "Unknown";
                    };

                    csvPrinter.printRecord(
                            session.getSessionId(),
                            session.getDriver(),
                            session.getDriverId(),
                            session.getStartDateTime(),
                            session.getEndDateTime(),
                            session.getDuration(),
                            session.getSafetyScore(),
                            session.getAlertsNumber(),
                            riskLevel
                    );
                }
            }
            csvPrinter.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Sessions CSV report", e);
        }
    }

    @Cacheable(value = "reports:drivers", key = "#companyId + ':' + #filter + ':' + (#from != null ? #from : '') + ':' + (#to != null ? #to : '') + ':' + (#search != null ? #search : '') + ':' + (#sort != null ? #sort : '')")
    public String generateDriversCSV(UUID companyId, String filter, String from, String to, String search, String sort) {
        var driversResponse = newAdminDriversAnalyticsService.buildDriversList(companyId, filter, from, to, search, sort);
        List<DriverSummaryDto> drivers = driversResponse.getDrivers();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Driver Name", "Driver ID", "Email", "Phone", "Joined At",
                        "Status", "Safety Score", "Total Sessions", "Last Session", "Risk Level")
                .build();

        StringWriter writer = new StringWriter();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            if (drivers != null) {
                for (DriverSummaryDto driver : drivers) {
                    String riskLevel = switch (driver.getRiskId()) {
                        case 0 -> "Low Risk";
                        case 1 -> "Medium Risk";
                        case 2 -> "High Risk";
                        default -> "Unknown";
                    };

                    csvPrinter.printRecord(
                            driver.getName(),
                            driver.getId(),
                            driver.getEmail(),
//                            driver.getPhone(),
                            driver.getJoinedAt(),
                            driver.getStatus(),
                            driver.getSafetyScore(),
                            driver.getTotalSessions(),
                            driver.getLastSession() != null ? driver.getLastSession() : "N/A",
                            riskLevel
                    );
                }
            }
            csvPrinter.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Drivers CSV report", e);
        }
    }
}
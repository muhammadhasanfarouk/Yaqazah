package com.yaqazah.adminAnalytics.service;

import com.yaqazah.adminAnalytics.dto.*;
import com.yaqazah.adminAnalytics.repository.AdminAnalyticsRepository;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.user.model.Role;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class NewAdminSessionAnalyticsService {

    // ==========================================
    // SAFETY SCORE ALGORITHM CONFIGURATION
    // ==========================================
    private static final double K_FACTOR = 0.025;
    private static final double MIN_TRIP_HOURS = 0.0833; // 5-minute minimum threshold

    private record DensityMetric(
            UUID entityId,
            double durationHours,
            long low, long medium, long high, long critical
    ) {
    }

    private final AdminAnalyticsRepository repository;

    public NewAdminSessionAnalyticsService(AdminAnalyticsRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "admin:sessions", key = "#companyId + ':' + #filter + ':' + #fromIso + ':' + #toIso + ':' + (#search != null ? #search : '') + ':' + (#risk != null ? #risk : '') + ':' + (#sort != null ? #sort : '')")
    @Transactional(readOnly = true)
    public SessionsListResponseDto buildSessionsList(UUID companyId, String filter, String fromIso, String toIso, String search, String risk, String sort) {
        return buildSessionsList(companyId, filter, fromIso, toIso, search, risk, sort, 0, Integer.MAX_VALUE);
    }

    @Cacheable(value = "admin:sessions", key = "#companyId + ':' + #filter + ':' + #fromIso + ':' + #toIso + ':' + (#search != null ? #search : '') + ':' + (#risk != null ? #risk : '') + ':' + (#sort != null ? #sort : '') + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
//    @Async
    public SessionsListResponseDto buildSessionsList(UUID companyId, String filter, String fromIso, String toIso, String search, String risk, String sort, int page, int size) {
        DashboardFilterResolver.DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        // 1. Fetch pre-pooled density metrics for ALL sessions in the period in 2 efficient queries
        Map<UUID, DensityMetric> curMetricsMap = fetchSessionDensityMap(companyId, curStartIso, curEndExcl);
        Map<UUID, DensityMetric> prevMetricsMap = fetchSessionDensityMap(companyId, prevStartIso, prevEndExcl);

        long sessionsCur = repository.countSessions(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long sessionsPrev = repository.countSessions(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);
        long activeCur = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long activePrev = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);
        long alertsCur = repository.countTotalAlerts(companyId, curStartIso, curEndExcl);
        long alertsPrev = repository.countTotalAlerts(companyId, prevStartIso, prevEndExcl);

        // 2. Calculate company-wide averages by pooling the session metrics together
        Double avgScoreCur = calculatePooledCompanyScore(curMetricsMap.values(), sessionsCur);
        Double avgScorePrev = calculatePooledCompanyScore(prevMetricsMap.values(), sessionsPrev);

        // Resolve risk filter
        Integer targetRiskId = null;
        if (risk != null && !risk.trim().isEmpty()) {
            String riskLower = risk.toLowerCase().trim();
            if (riskLower.equals("low") || riskLower.equals("0")) {
                targetRiskId = 0;
            } else if (riskLower.equals("medium") || riskLower.equals("1")) {
                targetRiskId = 1;
            } else if (riskLower.equals("high") || riskLower.equals("2")) {
                targetRiskId = 2;
            }
        }

        // 3. Assemble and filter list using O(1) Memory Lookups instead of N+1 database queries
        List<SessionSummaryDto> sessions = new ArrayList<>();
        for (Object[] row : repository.findSessionsForCompanyInPeriod(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl)) {
            SessionSummaryDto dto = mapSessionRow(row, curMetricsMap);

            // Filter by driver name (case-insensitive substring search)
            if (search != null && !search.trim().isEmpty()) {
                if (dto.getDriver() == null || !dto.getDriver().toLowerCase().contains(search.toLowerCase().trim())) {
                    continue;
                }
            }

            // Filter by risk level
            if (targetRiskId != null) {
                if (dto.getRiskId() != targetRiskId) {
                    continue;
                }
            }

            sessions.add(dto);
        }

        // Sort sessions list by startDateTime (newest first or oldest first)
        boolean newestFirst = !"oldest".equalsIgnoreCase(sort) && !"asc".equalsIgnoreCase(sort);
        sessions.sort((s1, s2) -> {
            String t1 = s1.getStartDateTime() != null ? s1.getStartDateTime() : "";
            String t2 = s2.getStartDateTime() != null ? s2.getStartDateTime() : "";
            int comp = t1.compareTo(t2);
            return newestFirst ? -comp : comp;
        });

        // Apply pagination slice
        int totalElements = sessions.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<SessionSummaryDto> paginatedSessions = new ArrayList<>(sessions.subList(start, end));

        return SessionsListResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(List.of(
                        overviewStat("Total Sessions", sessionsCur, sessionsPrev, false),
                        overviewStat("Active Drivers", activeCur, activePrev, false),
                        overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                        overviewStat("Total Alerts", alertsCur, alertsPrev, false)))
                .sessions(paginatedSessions)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .build();
    }

    @Cacheable(value = "admin:session-detail", key = "#companyId + ':' + #sessionId")
    @Transactional(readOnly = true)
    public Optional<SessionDetailsResponseDto> buildSessionDetails(UUID companyId, UUID sessionId) {
        List<Object[]> sessionRow = repository.findSessionForCompany(sessionId, companyId, Role.FLEET_DRIVER);
        if (sessionRow.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = sessionRow.get(0);
        UUID sid = (UUID) row[0];
        String driverName = (String) row[1];
        UUID driverId = (UUID) row[2];
        String startTime = (String) row[3];
        String endTime = (String) row[4];
        float hours = row[5] == null ? 0f : ((Number) row[5]).floatValue();
        int alerts = row[6] == null ? 0 : ((Number) row[6]).intValue();

        int safetyScore = (int) Math.round(sessionSafetyScore(sid));
        int riskId = riskLevel(safetyScore);

        Map<Integer, Long> typeCounts = alertTypeCountsForSession(sid);

        SessionDetailDto session = SessionDetailDto.builder()
                .sessionId(sid.toString())
                .driver(driverName)
                .driverId(driverId.toString())
                .startDateTime(startTime)
                .endDateTime(endTime)
                .duration(formatDurationHours(hours))
                .safetyScore(safetyScore)
                .alertsNumber(alerts)
                .distractionCount(typeCounts.getOrDefault(2, 0L).intValue())
                .drowsyCount(typeCounts.getOrDefault(1, 0L).intValue())
                .sleepCount(typeCounts.getOrDefault(0, 0L).intValue())
                .riskId(riskId)
                .build();

        // findLogsForSession returns: eventId, timestamp, alertId, riskId, title, subtitle, snapshotUrl
        List<DetectionLogDto> logs = new ArrayList<>();
        for (Object[] logRow : repository.findLogsForSession(sid)) {
            logs.add(DetectionLogDto.builder()
                    .eventId(((UUID) logRow[0]).toString())
                    .timestamp((String) logRow[1])
                    .alertId(logRow[2] == null ? -1 : ((Number) logRow[2]).intValue())
                    .riskId(logRow[3] == null ? -1 : ((Number) logRow[3]).intValue())
                    .title((String) logRow[4])
                    .subtitle((String) logRow[5])
                    .snapshotUrl((String) logRow[6])
                    .build());
        }

        return Optional.of(SessionDetailsResponseDto.builder()
                .session(session)
                .logs(logs)
                .build());
    }

    // ==========================================
    // ALGORITHM & DENSITY CORE
    // ==========================================

    private double calculateDensityScore(long low, long med, long high, long crit, double durationHours) {
        double effectiveHours = Math.max(durationHours, MIN_TRIP_HOURS);
        long penaltyPoints = (low * 1L) + (med * 3L) + (high * 8L) + (crit * 20L);
        double eventDensity = penaltyPoints / effectiveHours;
        double score = 100.0 * Math.exp(-K_FACTOR * eventDensity);
        return Math.min(100.0, Math.max(0.0, score));
    }

    private double calculateDensityScore(DensityMetric m) {
        return calculateDensityScore(m.low(), m.medium(), m.high(), m.critical(), m.durationHours());
    }

    private DensityMetric parseMetricRow(Object[] row) {
        return new DensityMetric(
                UUID.fromString((String) row[0]),
                row[1] == null ? 0.0 : ((Number) row[1]).doubleValue(),
                row[2] == null ? 0L : ((Number) row[2]).longValue(),
                row[3] == null ? 0L : ((Number) row[3]).longValue(),
                row[4] == null ? 0L : ((Number) row[4]).longValue(),
                row[5] == null ? 0L : ((Number) row[5]).longValue()
        );
    }

    private Map<UUID, DensityMetric> fetchSessionDensityMap(UUID companyId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = repository.findBulkSessionSafetyDensityMetrics(
                companyId, Role.FLEET_DRIVER.name(), startIso, endIsoExclusive
        );
        Map<UUID, DensityMetric> map = new HashMap<>();
        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            map.put(m.entityId(), m);
        }
        return map;
    }

    private Double calculatePooledCompanyScore(Collection<DensityMetric> sessionMetrics, long totalSessionsCount) {
        if (sessionMetrics.isEmpty() || totalSessionsCount <= 0) return null;

        double pooledHours = 0.0;
        long pooledLow = 0, pooledMed = 0, pooledHigh = 0, pooledCrit = 0;

        for (DensityMetric m : sessionMetrics) {
            pooledHours += m.durationHours();
            pooledLow += m.low();
            pooledMed += m.medium();
            pooledHigh += m.high();
            pooledCrit += m.critical();
        }

        if (pooledHours <= 0) return 100.0;
        return calculateDensityScore(pooledLow, pooledMed, pooledHigh, pooledCrit, pooledHours);
    }

    private double sessionSafetyScore(UUID sessionId) {
        // Re-uses the exact same repository method we created for the Driver service!
        List<Object[]> rows = repository.findSingleSessionSafetyMetric(sessionId);
        if (rows == null || rows.isEmpty()) {
            return 100.0;
        }
        return calculateDensityScore(parseMetricRow(rows.get(0)));
    }

    private int riskLevel(double score) {
        if (score > 90.0) return 0;
        if (score >= 70.0) return 1;
        return 2;
    }

    private SessionSummaryDto mapSessionRow(Object[] row, Map<UUID, DensityMetric> metricsMap) {
        UUID sessionId = (UUID) row[0];
        String driverName = (String) row[1];
        UUID driverId = (UUID) row[2];
        String startTime = (String) row[3];
        float hours = row[5] == null ? 0f : ((Number) row[5]).floatValue();
        int alerts = row[6] == null ? 0 : ((Number) row[6]).intValue();
        int distraction = row[7] == null ? 0 : ((Number) row[7]).intValue();
        int drowsy = row[8] == null ? 0 : ((Number) row[8]).intValue();
        int sleep = row[9] == null ? 0 : ((Number) row[9]).intValue();

        DensityMetric metric = metricsMap.get(sessionId);
        double rawScore = (metric != null) ? calculateDensityScore(metric) : 100.0;
        int score = (int) Math.round(rawScore);

        return SessionSummaryDto.builder()
                .sessionId(sessionId != null ? sessionId.toString() : null)
                .driver(driverName)
                .driverId(driverId != null ? driverId.toString() : null)
                .startDateTime(startTime)
                .duration(formatDurationHours(hours))
                .safetyScore(score)
                .alertsNumber(alerts)
                .riskId(riskLevel(score))
                .distractionCount(distraction)
                .drowsyCount(drowsy)
                .sleepCount(sleep)
                .build();
    }

    // ==========================================
    // REPOSITORY & DTO HELPERS
    // ==========================================

    private Map<Integer, Long> alertTypeCountsForSession(UUID sessionId) {
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] row : repository.countAlertsByTypeForSession(sessionId)) {
            map.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private OverviewStatDto overviewStat(String label, long current, long previous, boolean asPercent) {
        return overviewStat(label, (double) current, (double) previous, asPercent, false);
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent) {
        return overviewStat(label, current, previous, asPercent, false);
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent, boolean asTime) {
        String valueStr = (current == null) ? "N/A" : formatOverviewValue(current, asPercent, asTime);
        String deltaStr = (current == null || previous == null) ? "N/A" : formatDeltaPercent(deltaPercent(current, previous));

        return OverviewStatDto.builder()
                .label(label)
                .value(valueStr)
                .delta(deltaStr)
                .build();
    }

    private String formatOverviewValue(double value, boolean asPercent, boolean asTime) {
        if (asTime) return formatDrivingHours(value);
        if (asPercent) return Math.round(value) + "%";
        return String.valueOf(Math.round(value));
    }

    private String formatDrivingHours(double hours) {
        int totalSeconds = (int) Math.round(hours * 3600);
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
    }

    private Double deltaPercent(double current, double previous) {
        if (previous == 0.0) return current == 0.0 ? 0.0 : 100.0;
        return ((current - previous) / previous) * 100.0;
    }

    private String formatDeltaPercent(double delta) {
        if (delta == 0.0) return "0%";
        String sign = delta > 0 ? "+" : "";
        if (Math.abs(delta - Math.round(delta)) < 0.05) return sign + Math.round(delta) + "%";
        return sign + String.format(Locale.US, "%.1f", delta) + "%";
    }

    private String formatDurationHours(float durationHours) {
        int totalMinutes = Math.round(durationHours * 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousEnd = fromDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(inclusiveDays - 1);
        return new LocalDate[]{previousStart, previousEnd};
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }
}
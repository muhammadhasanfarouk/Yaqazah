package com.yaqazah.userAnalytics.service;

import com.yaqazah.adminAnalytics.dto.*;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.userAnalytics.repository.UserAnalyticsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class NewUserAnalyticsService {

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

    private final UserAnalyticsRepository repository;

    public NewUserAnalyticsService(UserAnalyticsRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "user:sessions", key = "#userId + ':' + #filter + ':' + #fromIso + ':' + #toIso")
    @Transactional(readOnly = true)
    public SessionsListResponseDto buildSessionsList(UUID userId, String filter, String fromIso, String toIso) {
        return buildSessionsList(userId, filter, fromIso, toIso, 0, Integer.MAX_VALUE);
    }

    @Cacheable(value = "user:sessions", key = "#userId + ':' + #filter + ':' + #fromIso + ':' + #toIso + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public SessionsListResponseDto buildSessionsList(UUID userId, String filter, String fromIso, String toIso, int page, int size) {
        DashboardFilterResolver.DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        // 1. Fetch pre-pooled density metrics for the user's sessions in 2 efficient queries
        Map<UUID, DensityMetric> curMetricsMap = fetchUserSessionsDensityMap(userId, curStartIso, curEndExcl);
        Map<UUID, DensityMetric> prevMetricsMap = fetchUserSessionsDensityMap(userId, prevStartIso, prevEndExcl);

        long sessionsCur = repository.countSessionsForUser(userId, curStartIso, curEndExcl);
        long sessionsPrev = repository.countSessionsForUser(userId, prevStartIso, prevEndExcl);

        long alertsCur = repository.countTotalAlertsForUser(userId, curStartIso, curEndExcl);
        long alertsPrev = repository.countTotalAlertsForUser(userId, prevStartIso, prevEndExcl);

        // 2. Derive User Average Safety Score in-memory from the session pools (Zero extra DB queries)
        Double avgScoreCur = (sessionsCur == 0) ? null : calculatePooledUserScore(curMetricsMap.values(), sessionsCur);
        Double avgScorePrev = (sessionsPrev == 0) ? null : calculatePooledUserScore(prevMetricsMap.values(), sessionsPrev);

        // 3. Assemble the list using O(1) Memory Lookups
        List<SessionSummaryDto> sessions = new ArrayList<>();
        for (Object[] row : repository.findSessionsForUserInPeriod(userId, curStartIso, curEndExcl)) {
            sessions.add(mapSessionRow(row, curMetricsMap));
        }

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

    @Cacheable(value = "user:session-detail", key = "#userId + ':' + #sessionId")
    @Transactional(readOnly = true)
    public Optional<SessionDetailsResponseDto> buildSessionDetails(UUID userId, UUID sessionId) {
        Optional<Object> sessionRow = repository.findSessionForUser(sessionId, userId);
        if (sessionRow.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = (Object[]) sessionRow.get();
        UUID sid = (UUID) row[0];
        String startTime = (String) row[1];
        String endTime = (String) row[2];
        float hours = row[3] == null ? 0f : ((Number) row[3]).floatValue();
        int alerts = row[4] == null ? 0 : ((Number) row[4]).intValue();

        int safetyScore = (int) Math.round(sessionSafetyScore(sid));
        int riskId = riskLevel(safetyScore);

        Map<Integer, Long> typeCounts = alertTypeCountsForSession(sid);

        SessionDetailDto session = SessionDetailDto.builder()
                .sessionId(sid.toString())
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

    private Map<UUID, DensityMetric> fetchUserSessionsDensityMap(UUID userId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = repository.findBulkUserSessionsDensityMetrics(userId, startIso, endIsoExclusive);
        Map<UUID, DensityMetric> map = new HashMap<>();
        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            map.put(m.entityId(), m);
        }
        return map;
    }

    private Double calculatePooledUserScore(Collection<DensityMetric> sessionMetrics, long totalSessionsCount) {
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
        String startTime = (String) row[1];
        float hours = row[2] == null ? 0f : ((Number) row[2]).floatValue();
        int alerts = row[3] == null ? 0 : ((Number) row[3]).intValue();

        DensityMetric metric = metricsMap.get(sessionId);
        double rawScore = (metric != null) ? calculateDensityScore(metric) : 100.0;
        int score = (int) Math.round(rawScore);

        return SessionSummaryDto.builder()
                .sessionId(sessionId != null ? sessionId.toString() : null)
                .startDateTime(startTime)
                .duration(formatDurationHours(hours))
                .safetyScore(score)
                .alertsNumber(alerts)
                .riskId(riskLevel(score))
                .driverId(row[4] != null ? String.valueOf(row[4]) : null)
                .driver(row[5] != null ? (String) row[5] : null)
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
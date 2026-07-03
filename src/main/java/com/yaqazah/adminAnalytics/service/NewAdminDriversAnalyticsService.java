package com.yaqazah.adminAnalytics.service;

import com.yaqazah.adminAnalytics.dto.DriverDetailResponseDto;
import com.yaqazah.adminAnalytics.dto.DriverSummaryDto;
import com.yaqazah.adminAnalytics.dto.DriversListResponseDto;
import com.yaqazah.adminAnalytics.dto.SessionSummaryDto;
import com.yaqazah.adminAnalytics.repository.AdminAnalyticsRepository;
import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.dashboard.util.DashboardFilterResolver.DateRange;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class NewAdminDriversAnalyticsService {

    private static final DateTimeFormatter JOINED_AT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter SESSION_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // ==========================================
    // SAFETY SCORE ALGORITHM CONFIGURATION
    // ==========================================
    private static final double K_FACTOR = 0.025;
    private static final double MIN_TRIP_HOURS = 0.0833; // 5-minute threshold

    public enum TrendGranularity {HOURLY, DAILY, MONTHLY, YEARLY}

    private record TrendResolution(TrendGranularity granularity, List<String> sqlKeys, List<String> displayLabels) {
    }

    private record DensityMetric(
            UUID entityId,
            double durationHours,
            long low, long medium, long high, long critical
    ) {
    }

    private final AdminAnalyticsRepository repository;
    private final UserRepository userRepository;

    public NewAdminDriversAnalyticsService(AdminAnalyticsRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "admin:drivers", key = "#companyId + ':' + #filter + ':' + #fromIso + ':' + #toIso + ':' + (#search != null ? #search : '') + ':' + (#sort != null ? #sort : '')")
    @Transactional(readOnly = true)
    public DriversListResponseDto buildDriversList(UUID companyId, String filter, String fromIso, String toIso, String search, String sort) {
        return buildDriversList(companyId, filter, fromIso, toIso, search, sort, 0, Integer.MAX_VALUE);
    }

    @Cacheable(value = "admin:drivers", key = "#companyId + ':' + #filter + ':' + #fromIso + ':' + #toIso + ':' + (#search != null ? #search : '') + ':' + (#sort != null ? #sort : '') + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public DriversListResponseDto buildDriversList(UUID companyId, String filter, String fromIso, String toIso, String search, String sort, int page, int size) {
        DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        // 1. Fetch pre-pooled density metrics for the entire company in 2 efficient bulk queries
        Map<UUID, DensityMetric> curMetricsMap = fetchDriverDensityMap(companyId, curStartIso, curEndExcl);
        Map<UUID, DensityMetric> prevMetricsMap = fetchDriverDensityMap(companyId, prevStartIso, prevEndExcl);

        long totalDrivers = userRepository.countByCompany_CompanyIdAndRole(companyId, Role.FLEET_DRIVER);
        long activeCur = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long activePrev = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);

        Double avgScoreCur = calculatePooledCompanyScore(curMetricsMap.values(), activeCur);
        Double avgScorePrev = calculatePooledCompanyScore(prevMetricsMap.values(), activePrev);

        // 2. Map all drivers using instant O(1) Memory Lookups and track their last session times
        List<DriverSummaryDto> allDrivers = new ArrayList<>();
        Map<String, String> driverLastSessionTime = new HashMap<>();
        for (Object[] row : repository.findFleetDriversForCompany(companyId, Role.FLEET_DRIVER)) {
            UUID userId = (UUID) row[0];
            List<String> lastStarts = repository.findLastSessionStartTime(userId, PageRequest.of(0, 1));
            String rawTime = lastStarts.isEmpty() ? "" : lastStarts.get(0);
            driverLastSessionTime.put(userId.toString(), rawTime);
            allDrivers.add(mapDriverRow(row, curMetricsMap, lastStarts));
        }

        long highRiskCur = allDrivers.stream().filter(d -> d.getRiskId() == 2).count();
        long highRiskPrev = countHighRiskMapEntries(prevMetricsMap);

        // Apply search filter (driver name)
        List<DriverSummaryDto> filteredDrivers = new ArrayList<>();
        for (DriverSummaryDto dto : allDrivers) {
            if (search != null && !search.trim().isEmpty()) {
                if (dto.getName() == null || !dto.getName().toLowerCase().contains(search.toLowerCase().trim())) {
                    continue;
                }
            }
            filteredDrivers.add(dto);
        }

        // Sort drivers by raw last session start time (newest first or oldest first), placing "Never" at the bottom
        boolean newestFirst = !"oldest".equalsIgnoreCase(sort) && !"asc".equalsIgnoreCase(sort);
        filteredDrivers.sort((d1, d2) -> {
            String t1 = driverLastSessionTime.getOrDefault(d1.getId(), "");
            String t2 = driverLastSessionTime.getOrDefault(d2.getId(), "");

            if (t1.isEmpty() && t2.isEmpty()) return 0;
            if (t1.isEmpty()) return 1;  // Put "Never" at the bottom
            if (t2.isEmpty()) return -1; // Put "Never" at the bottom

            int comp = t1.compareTo(t2);
            return newestFirst ? -comp : comp;
        });

        // Apply pagination slice
        int totalElements = filteredDrivers.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<DriverSummaryDto> paginatedDrivers = new ArrayList<>(filteredDrivers.subList(start, end));

        return DriversListResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(List.of(
                        overviewStat("Total Drivers", totalDrivers, totalDrivers, false),
                        overviewStat("Active Drivers", activeCur, activePrev, false),
                        overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                        overviewStat("High Risk Drivers", highRiskCur, highRiskPrev, false)))
                .drivers(paginatedDrivers)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .build();
    }

    @Cacheable(value = "admin:driver-detail", key = "#companyId + ':' + #driverId + ':' + #filter + ':' + #fromIso + ':' + #toIso")
    @Transactional(readOnly = true)
    public Optional<DriverDetailResponseDto> buildDriverDetail(
            UUID companyId, UUID driverId, String filter, String fromIso, String toIso) {

        List<Object[]> driverRow = repository.findDriverForCompany(driverId, companyId, Role.FLEET_DRIVER);
        if (driverRow.isEmpty()) {
            return Optional.empty();
        }

        DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        TrendResolution resolution = resolveTrendBuckets(filter, range.from(), range.to());

        List<Object[]> sessionsInPeriod = repository.findSessionsForDriverInPeriod(driverId, curStartIso, curEndExcl);
        List<Object[]> sessionsPrev = repository.findSessionsForDriverInPeriod(driverId, prevStartIso, prevEndExcl);

        long sessionsCur = sessionsInPeriod.size();
        long sessionsPrevLong = sessionsPrev.size();
        long highRiskCur = sessionsInPeriod.stream()
                .mapToLong(r -> riskLevel(sessionSafetyScore((UUID) r[0])) == 2 ? 1 : 0).sum();
        long highRiskPrev = sessionsPrev.stream()
                .mapToLong(r -> riskLevel(sessionSafetyScore((UUID) r[0])) == 2 ? 1 : 0).sum();

        long alertsCur = repository.countAlertsForDriver(driverId, curStartIso, curEndExcl);
        long alertsPrev = repository.countAlertsForDriver(driverId, prevStartIso, prevEndExcl);
        double hoursCur = repository.sumDrivingHoursForDriver(driverId, curStartIso, curEndExcl);
        double hoursPrev = repository.sumDrivingHoursForDriver(driverId, prevStartIso, prevEndExcl);

        // Fetch company map to resolve the single driver's top-level summary DTO
        Map<UUID, DensityMetric> curCompanyMap = fetchDriverDensityMap(companyId, curStartIso, curEndExcl);
        Object[] driverRowArray = driverRow.get(0);
        DriverSummaryDto selected = mapDriverRow(driverRowArray, curCompanyMap);

        List<SessionSummaryDto> sessions = new ArrayList<>();
        Map<String, List<Integer>> scoresBySqlKey = new HashMap<>();

        for (Object[] row : sessionsInPeriod) {
            UUID sid = (UUID) row[0];
            String rawStartTimeIso = (String) row[1];
            float hours = row[2] == null ? 0f : ((Number) row[2]).floatValue();
            int alerts = row[3] == null ? 0 : ((Number) row[3]).intValue();
            int distraction = row[4] == null ? 0 : ((Number) row[4]).intValue();
            int drowsy = row[5] == null ? 0 : ((Number) row[5]).intValue();
            int sleep = row[6] == null ? 0 : ((Number) row[6]).intValue();

            int score = (int) Math.round(sessionSafetyScore(sid));

            sessions.add(SessionSummaryDto.builder()
                    .sessionId(sid.toString())
                    .driver(selected.getName())
                    .driverId(driverId.toString())
                    .startDateTime(extractDate(rawStartTimeIso))
                    .duration(formatDurationHours(hours))
                    .safetyScore(score)
                    .alertsNumber(alerts)
                    .riskId(riskLevel(score))
                    .distractionCount(distraction)
                    .drowsyCount(drowsy)
                    .sleepCount(sleep)
                    .build());

            String bucketKey = formatTimestampToBucketKey(rawStartTimeIso, resolution.granularity());
            scoresBySqlKey.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(score);
        }

        List<Integer> performanceTrend = new ArrayList<>(resolution.sqlKeys().size());
        for (String expectedSqlKey : resolution.sqlKeys()) {
            List<Integer> bucketScores = scoresBySqlKey.get(expectedSqlKey);
            if (bucketScores == null || bucketScores.isEmpty()) {
                performanceTrend.add(100);
            } else {
                double avgBucketScore = bucketScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                performanceTrend.add((int) Math.round(avgBucketScore));
            }
        }

        List<AlertTrendValueDto> alertTrendValues = buildDriverAlertTrendValues(driverId, curStartIso, curEndExcl, resolution);
        List<PieDistributionDto> pieDistribution = buildDriverPieDistribution(driverId, curStartIso, curEndExcl);
        List<RiskDistributionDto> riskDistribution = buildDriverRiskDistribution(sessionsInPeriod);
        List<String> trendLabels = resolution.displayLabels();

        if ("4".equals(DashboardFilterResolver.toFilterId(filter))) {
            com.yaqazah.dashboard.util.WeeklyAggregationUtil.WeeklyAggregationResult aggregated = com.yaqazah.dashboard.util.WeeklyAggregationUtil.aggregateMonthToWeeks(
                    trendLabels, performanceTrend, alertTrendValues);
            performanceTrend = aggregated.performanceTrend();
            alertTrendValues = aggregated.alertTrendValues();
            trendLabels = aggregated.trendLabels();
        }

        return Optional.of(DriverDetailResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(List.of(
                        overviewStat("Total Sessions", sessionsCur, sessionsPrevLong, false),
                        overviewStat("Total High Risk Sessions", highRiskCur, highRiskPrev, false),
                        overviewStat("Total Alerts", alertsCur, alertsPrev, false),
                        overviewStat("Driving Hours", hoursCur, hoursPrev, false, true)))
                .selectedDriver(selected)
                .performanceTrend(performanceTrend)
                .alertTrendValues(alertTrendValues)
                .pieDistribution(pieDistribution)
                .riskDistribution(riskDistribution)
                .sessions(sessions)
                .trendLabels(trendLabels)
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

    private Map<UUID, DensityMetric> fetchDriverDensityMap(UUID companyId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = repository.findDriverSafetyDensityMetrics(
                companyId, Role.FLEET_DRIVER.name(), startIso, endIsoExclusive
        );
        Map<UUID, DensityMetric> map = new HashMap<>();
        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            map.put(m.entityId(), m);
        }
        return map;
    }

    private Double calculatePooledCompanyScore(Collection<DensityMetric> activeMetrics, long totalActiveDrivers) {
        if (activeMetrics.isEmpty() || totalActiveDrivers <= 0) return null;

        double pooledHours = 0.0;
        long pooledLow = 0, pooledMed = 0, pooledHigh = 0, pooledCrit = 0;

        for (DensityMetric m : activeMetrics) {
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

    private long countHighRiskMapEntries(Map<UUID, DensityMetric> map) {
        return map.values().stream()
                .filter(m -> riskLevel(calculateDensityScore(m)) == 2)
                .count();
    }

    private DriverSummaryDto mapDriverRow(Object[] row, Map<UUID, DensityMetric> metricsMap) {
        UUID userId = (UUID) row[0];
        List<String> lastStarts = repository.findLastSessionStartTime(userId, PageRequest.of(0, 1));
        return mapDriverRow(row, metricsMap, lastStarts);
    }

    private DriverSummaryDto mapDriverRow(Object[] row, Map<UUID, DensityMetric> metricsMap, List<String> lastStarts) {
        UUID userId = (UUID) row[0];
        String name = (String) row[1];
        String email = (String) row[2];
        UserStatus status = (UserStatus) row[3];
        Instant insertedAt = (Instant) row[4];

        DensityMetric metric = metricsMap.get(userId);
        double rawScore = (metric != null) ? calculateDensityScore(metric) : 100.0;
        int score = (int) Math.round(rawScore);

        return DriverSummaryDto.builder()
                .name(name)
                .id(userId.toString())
                .email(email)
                .riskId(riskLevel(score))
                .safetyScore(score)
                .totalSessions(repository.countSessionsForDriver(userId))
                .lastSession(lastStarts.isEmpty() ? "Never" : formatRelativeTime(lastStarts.get(0)))
                .joinedAt(JOINED_AT_FMT.format(insertedAt.atZone(ZoneOffset.UTC)))
                .status(formatUserStatus(status))
                .build();
    }

    // ==========================================
    // REPOSITORY & DTO HELPERS
    // ==========================================

    private List<AlertTrendValueDto> buildDriverAlertTrendValues(
            UUID driverId, String startIso, String endIsoExclusive, TrendResolution resolution) {

        List<Object[]> rows = switch (resolution.granularity()) {
            case HOURLY -> repository.countDriverAlertsHourly(driverId, startIso, endIsoExclusive);
            case DAILY -> repository.countDriverAlertsDaily(driverId, startIso, endIsoExclusive);
            case MONTHLY, YEARLY -> repository.countDriverAlertsMonthly(driverId, startIso, endIsoExclusive);
        };

        Map<String, Map<Integer, Long>> countMap = new HashMap<>();
        for (Object[] row : rows) {
            String bucketKey = (String) row[0];
            if (resolution.granularity() == TrendGranularity.YEARLY && bucketKey != null && bucketKey.length() >= 4) {
                bucketKey = bucketKey.substring(0, 4);
            }
            int alertId = ((Number) row[1]).intValue();

            // Trend types: 0=Asleep, 1=Drowsy, 2=Distracted
            if (alertId < 0 || alertId > 2) continue;

            long count = ((Number) row[2]).longValue();
            countMap.computeIfAbsent(bucketKey, k -> new HashMap<>()).merge(alertId, count, Long::sum);
        }

        long[] grandTotals = new long[3];
        List<AlertTrendValueDto> out = new ArrayList<>();

        for (int typeId = 0; typeId <= 2; typeId++) {
            List<Long> paddedValues = new ArrayList<>(resolution.sqlKeys().size());
            for (String expectedKey : resolution.sqlKeys()) {
                long val = countMap.getOrDefault(expectedKey, Collections.emptyMap()).getOrDefault(typeId, 0L);
                paddedValues.add(val);
                grandTotals[typeId] += val;
            }
            out.add(AlertTrendValueDto.builder().id(typeId).values(paddedValues).percent(0).build());
        }

        long totalTrendAlerts = grandTotals[0] + grandTotals[1] + grandTotals[2];
        for (int i = 0; i < out.size(); i++) {
            int pct = totalTrendAlerts == 0 ? 0 : (int) Math.round((grandTotals[i] * 100.0) / totalTrendAlerts);
            out.get(i).setPercent(pct);
        }
        return out;
    }

    private List<PieDistributionDto> buildDriverPieDistribution(UUID driverId, String startIso, String endIsoExclusive) {
        long[] perId = new long[3];
        long mappedTotal = 0;
        // rows: [alertId, count] — pie types: 3=Phone, 4=LookingAway, 5=EatingAndDrinking
        for (Object[] row : repository.countAlertsByTypeForDriver(driverId, startIso, endIsoExclusive)) {
            int alertId = ((Number) row[0]).intValue();
            long c = ((Number) row[1]).longValue();
            if (alertId < 3 || alertId > 5) continue;
            perId[alertId - 3] += c;
            mappedTotal += c;
        }

        if (mappedTotal == 0) {
            return Collections.emptyList();
        }

        List<PieDistributionDto> slices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (perId[i] == 0) continue;
            int typeId = i + 3;
            int percent = (int) Math.round(perId[i] * 100.0 / mappedTotal);
            slices.add(PieDistributionDto.builder().id(typeId).percent(percent).build());
        }
        return slices;
    }

    private List<RiskDistributionDto> buildDriverRiskDistribution(List<Object[]> sessions) {
        long[] bucketCounts = new long[3];
        for (Object[] row : sessions) {
            UUID sid = (UUID) row[0];
            bucketCounts[riskLevel(sessionSafetyScore(sid))]++;
        }

        // Calculate the total number of sessions
        long totalSessions = sessions.size();

        List<RiskDistributionDto> out = new ArrayList<>();
        for (int level = 0; level < 3; level++) {
            // Calculate percentage, guarding against division by zero
            long percentage = 0;
            if (totalSessions > 0) {
                // Using Math.round to get accurate rounding for the percentage
                percentage = Math.round((bucketCounts[level] * 100.0) / totalSessions);
            }

            out.add(RiskDistributionDto.builder()
                    .id(level)
                    .value(percentage) // Return percentage instead of raw count
                    .build());
        }
        return out;
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

    private String formatRelativeTime(String isoTimestamp) {
        try {
            Instant instant = Instant.parse(isoTimestamp);
            Duration ago = Duration.between(instant, Instant.now());
            long minutes = ago.toMinutes();
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            long hours = ago.toHours();
            if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
            long days = ago.toDays();
            return days + (days == 1 ? " day ago" : " days ago");
        } catch (Exception ex) {
            return isoTimestamp;
        }
    }

    private String formatUserStatus(UserStatus status) {
        if (status == null) return "Active";
        return switch (status) {
            case ACTIVE -> "Active";
            case PENDING_VERIFICATION -> "Under Review";
        };
    }

    private String extractDate(String isoTimestamp) {
        try {
            return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).format(SESSION_DATE_FMT);
        } catch (Exception ex) {
            return isoTimestamp.length() >= 10 ? isoTimestamp.substring(0, 10) : isoTimestamp;
        }
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

    private String formatTimestampToBucketKey(String isoTimestamp, TrendGranularity granularity) {
        if (isoTimestamp == null) return "";
        return switch (granularity) {
            case HOURLY -> isoTimestamp.length() >= 13 ? isoTimestamp.substring(0, 13) : isoTimestamp;
            case DAILY -> isoTimestamp.length() >= 10 ? isoTimestamp.substring(0, 10) : isoTimestamp;
            case MONTHLY -> isoTimestamp.length() >= 7 ? isoTimestamp.substring(0, 7) : isoTimestamp;
            case YEARLY -> isoTimestamp.length() >= 4 ? isoTimestamp.substring(0, 4) : isoTimestamp;
        };
    }

    private TrendResolution resolveTrendBuckets(String filterId, LocalDate from, LocalDate to) {
        List<String> sqlKeys = new ArrayList<>();
        List<String> displayLabels = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(from, to) + 1;

        if (from.plusYears(2).isBefore(to)) {
            for (int y = from.getYear(); y <= to.getYear(); y++) {
                sqlKeys.add(String.valueOf(y));
                displayLabels.add(String.valueOf(y));
            }
            return new TrendResolution(TrendGranularity.YEARLY, sqlKeys, displayLabels);
        }

        if ("1".equals(filterId) || "2".equals(filterId) || days <= 1) {
            for (int h = 0; h < 24; h++) {
                sqlKeys.add(String.format("%sT%02d", from, h));
                displayLabels.add(String.format("%02d:00", h));
            }
            return new TrendResolution(TrendGranularity.HOURLY, sqlKeys, displayLabels);
        }

        if ("3".equals(filterId)) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                sqlKeys.add(d.toString());
                displayLabels.add(d.getDayOfWeek().name().substring(0, 3));
            }
            return new TrendResolution(TrendGranularity.DAILY, sqlKeys, displayLabels);
        }

        if ("4".equals(filterId)) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                sqlKeys.add(d.toString());
                displayLabels.add(String.valueOf(d.getDayOfMonth()));
            }
            return new TrendResolution(TrendGranularity.DAILY, sqlKeys, displayLabels);
        }

        if (days <= 35) {
            DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US);
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                sqlKeys.add(d.toString());
                displayLabels.add(d.format(shortDate));
            }
            return new TrendResolution(TrendGranularity.DAILY, sqlKeys, displayLabels);
        } else {
            java.time.YearMonth current = java.time.YearMonth.from(from);
            java.time.YearMonth end = java.time.YearMonth.from(to);
            DateTimeFormatter shortMonth = DateTimeFormatter.ofPattern("MMM yy", Locale.US);
            while (!current.isAfter(end)) {
                sqlKeys.add(current.toString());
                displayLabels.add(current.atDay(1).format(shortMonth));
                current = current.plusMonths(1);
            }
            return new TrendResolution(TrendGranularity.MONTHLY, sqlKeys, displayLabels);
        }
    }
}
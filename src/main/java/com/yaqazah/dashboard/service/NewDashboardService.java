package com.yaqazah.dashboard.service;

import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.DashboardResponseDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RecentSessionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.dto.TopPerformerDto;
import com.yaqazah.dashboard.repository.DashboardRepository;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.dashboard.util.DashboardFilterResolver.DateRange;
import com.yaqazah.user.model.Role;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewDashboardService {

    public enum TrendGranularity {
        HOURLY, DAILY, MONTHLY, YEARLY
    }

    private static final DateTimeFormatter TREND_DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int TREND_DAYS = 7;
    private static final int RECENT_SESSION_LIMIT = 3;
    private static final int TOP_PERFORMER_LIMIT = 3;

    // ==========================================
    // SAFETY SCORE ALGORITHM CONFIGURATION
    // ==========================================
    private static final double K_FACTOR = 0.025;
    private static final double MIN_TRIP_HOURS = 0.0833; // 5-minute threshold

    private record TrendResolution(
            TrendGranularity granularity,
            List<String> sqlKeys,
            List<String> displayLabels) {
    }

    private record DensityMetric(
            UUID entityId,
            double durationHours,
            long low, long medium, long high, long critical) {
    }

    private final DashboardRepository dashboardRepository;

    public NewDashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Cacheable(value = "dashboard", key = "#companyId + ':' + #filter + ':' + #fromIso + ':' + #toIso")
    @Transactional(readOnly = true)
    public DashboardResponseDto buildDashboard(UUID companyId, String filter, String fromIso, String toIso) {
        DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        LocalDate fromDate = range.from();
        LocalDate toDate = range.to();

        LocalDate[] previous = previousInclusiveRange(fromDate, toDate);
        LocalDate prevFrom = previous[0];
        LocalDate prevTo = previous[1];

        String curStartIso = startOfDayUtcIso(fromDate);
        String curEndExcl = startOfDayUtcIso(toDate.plusDays(1));
        String prevStartIso = startOfDayUtcIso(prevFrom);
        String prevEndExcl = startOfDayUtcIso(prevTo.plusDays(1));

        // 1. Resolve our X-Axis contract
        String apiFilterId = DashboardFilterResolver.toFilterId(filter);
        TrendResolution resolution = resolveTrendBuckets(apiFilterId, fromDate, toDate);

        // 2. Pass it to the trend builder
        List<AlertTrendValueDto> alertTrendValues = buildAlertTrendValues(
                companyId, curStartIso, curEndExcl, resolution);

        long sessionsCur = dashboardRepository.countSessions(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long sessionsPrev = dashboardRepository.countSessions(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);

        long activeCur = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long activePrev = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso,
                prevEndExcl);

        long alertsCur = dashboardRepository.countTotalAlerts(companyId, curStartIso, curEndExcl);
        long alertsPrev = dashboardRepository.countTotalAlerts(companyId, prevStartIso, prevEndExcl);

        Double avgScoreCur = companyAverageSafetyScore(companyId, curStartIso, curEndExcl);
        Double avgScorePrev = companyAverageSafetyScore(companyId, prevStartIso, prevEndExcl);

        List<OverviewStatDto> overviewStats = List.of(
                overviewStat("Total Sessions", sessionsCur, sessionsPrev, false),
                overviewStat("Active Drivers", activeCur, activePrev, false),
                overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                overviewStat("Total Alerts", alertsCur, alertsPrev, false));

        List<PieDistributionDto> pieDistribution = buildPieDistribution(companyId, curStartIso, curEndExcl);
        List<RiskDistributionDto> riskDistribution = buildRiskDistribution(companyId, curStartIso, curEndExcl);
        List<RecentSessionDto> recentSessions = buildRecentSessions(companyId);
        List<TopPerformerDto> topPerformers = buildTopPerformers(companyId, curStartIso, curEndExcl);
        List<String> trendLabels = resolution.displayLabels();

        if ("4".equals(apiFilterId)) {
            com.yaqazah.dashboard.util.WeeklyAggregationUtil.WeeklyAggregationResult aggregated = com.yaqazah.dashboard.util.WeeklyAggregationUtil.aggregateMonthToWeeks(
                    trendLabels, null, alertTrendValues);
            alertTrendValues = aggregated.alertTrendValues();
            trendLabels = aggregated.trendLabels();
        }

        return DashboardResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(fromDate, toDate))
                .overviewStats(overviewStats)
                .alertTrendValues(alertTrendValues)
                .pieDistribution(pieDistribution)
                .riskDistribution(riskDistribution)
                .recentSessions(recentSessions)
                .topPerformers(topPerformers)
                .trendLabels(trendLabels)
                .build();
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent) {
        String deltaStr = (current == null || previous == null) ? "N/A"
                : formatDeltaPercent(deltaPercent(current, previous));
        return OverviewStatDto.builder()
                .label(label)
                .value(formatOverviewValue(current, asPercent))
                .delta(deltaStr)
                .build();
    }

    private OverviewStatDto overviewStat(String label, long current, long previous, boolean asPercent) {
        return overviewStat(label, (double) current, (double) previous, asPercent);
    }

    private String formatOverviewValue(Double value, boolean asPercent) {
        if (value == null) {
            return "N/A";
        }
        if (asPercent) {
            return Math.round(value) + "%";
        }
        return String.valueOf(Math.round(value));
    }

    private Double deltaPercent(Double current, Double previous) {
        if (previous == null || previous == 0.0) {
            return (current == null || current == 0.0) ? 0.0 : 100.0;
        }
        if (current == null) {
            return -100.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    private String formatDeltaPercent(double delta) {
        if (delta == 0.0) {
            return "0%";
        }
        String sign = delta > 0 ? "+" : "";
        if (Math.abs(delta - Math.round(delta)) < 0.05) {
            return sign + Math.round(delta) + "%";
        }
        return sign + String.format(Locale.US, "%.1f", delta) + "%";
    }

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousEnd = fromDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(inclusiveDays - 1);
        return new LocalDate[] { previousStart, previousEnd };
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }

    /**
     * Algorithmic Core: Scientific Event Density Calculation
     */
    private double calculateDensityScore(long low, long med, long high, long crit, double durationHours) {
        double effectiveHours = Math.max(durationHours, MIN_TRIP_HOURS);
        long penaltyPoints = (low * 1L) + (med * 3L) + (high * 8L) + (crit * 20L);
        double eventDensity = penaltyPoints / effectiveHours;
        double score = 100.0 * Math.exp(-K_FACTOR * eventDensity);
        return Math.min(100.0, Math.max(0.0, score));
    }

    private DensityMetric parseMetricRow(Object[] row) {
        return new DensityMetric(
                UUID.fromString((String) row[0]),
                row[1] == null ? 0.0 : ((Number) row[1]).doubleValue(),
                row[2] == null ? 0L : ((Number) row[2]).longValue(),
                row[3] == null ? 0L : ((Number) row[3]).longValue(),
                row[4] == null ? 0L : ((Number) row[4]).longValue(),
                row[5] == null ? 0L : ((Number) row[5]).longValue());
    }

    private Double companyAverageSafetyScore(UUID companyId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = dashboardRepository.findDriverSafetyDensityMetrics(
                companyId, Role.FLEET_DRIVER.name(), startIso, endIsoExclusive);

        if (rows.isEmpty())
            return 100.0;

        double pooledHours = 0.0;
        long pooledLow = 0, pooledMed = 0, pooledHigh = 0, pooledCrit = 0;

        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            pooledHours += m.durationHours();
            pooledLow += m.low();
            pooledMed += m.medium();
            pooledHigh += m.high();
            pooledCrit += m.critical();
        }

        if (pooledHours <= 0)
            return 100.0;

        return calculateDensityScore(pooledLow, pooledMed, pooledHigh, pooledCrit, pooledHours);
    }

    private List<AlertTrendValueDto> buildAlertTrendValues(
            UUID companyId,
            String startIso,
            String endIsoExclusive,
            TrendResolution resolution) {

        List<Object[]> rows = switch (resolution.granularity()) {
            case HOURLY -> dashboardRepository.countAlertsHourly(companyId, startIso, endIsoExclusive);
            case DAILY -> dashboardRepository.countAlertsDaily(companyId, startIso, endIsoExclusive);
            case MONTHLY, YEARLY -> dashboardRepository.countAlertsMonthly(companyId, startIso, endIsoExclusive);
        };

        Map<String, Map<Integer, Long>> countMap = new HashMap<>();
        for (Object[] row : rows) {
            String bucketKey = (String) row[0];
            if (resolution.granularity() == TrendGranularity.YEARLY && bucketKey != null && bucketKey.length() >= 4) {
                bucketKey = bucketKey.substring(0, 4);
            }
            int alertId = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            if (alertId < 0 || alertId > 2)
                continue;
            countMap.computeIfAbsent(bucketKey, k -> new HashMap<>())
                    .merge(alertId, count, Long::sum);
        }

        long[] grandTotals = new long[3];
        List<AlertTrendValueDto> out = new ArrayList<>();

        for (int typeId = 0; typeId <= 2; typeId++) {
            List<Long> paddedValues = new ArrayList<>(resolution.sqlKeys().size());

            for (String expectedKey : resolution.sqlKeys()) {
                long val = countMap.getOrDefault(expectedKey, Collections.emptyMap())
                        .getOrDefault(typeId, 0L);
                paddedValues.add(val);
                grandTotals[typeId] += val;
            }

            out.add(AlertTrendValueDto.builder()
                    .id(typeId)
                    .values(paddedValues)
                    .percent(0)
                    .build());
        }

        long totalTrendAlerts = grandTotals[0] + grandTotals[1] + grandTotals[2];
        for (int i = 0; i < out.size(); i++) {
            int pct = totalTrendAlerts == 0 ? 0 : (int) Math.round((grandTotals[i] * 100.0) / totalTrendAlerts);
            out.get(i).setPercent(pct);
        }

        return out;
    }

    private List<PieDistributionDto> buildPieDistribution(UUID companyId, String startIso, String endIsoExclusive) {
        long[] countsById = new long[6];
        long totalAlerts = 0;

        for (Object[] row : dashboardRepository.countAlertsByType(companyId, startIso, endIsoExclusive)) {
            if (row[0] == null || row[1] == null)
                continue; // Defensive null guard

            int alertId = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();

            if (alertId >= 3 && alertId <= 5) {
                countsById[alertId] += count;
                totalAlerts += count;
            }
        }

        if (totalAlerts == 0) {
            return Collections.emptyList();
        }

        List<PieDistributionDto> slices = new ArrayList<>();
        for (int id = 3; id <= 5; id++) {
            long count = countsById[id];
            if (count == 0)
                continue;

            int percent = (int) Math.round((count * 100.0) / totalAlerts);
            slices.add(PieDistributionDto.builder()
                    .id(id)
                    .percent(percent)
                    .build());
        }

        return slices;
    }

    private List<RiskDistributionDto> buildRiskDistribution(UUID companyId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = dashboardRepository.findSessionSafetyDensityMetrics(
                companyId, Role.FLEET_DRIVER.name(), startIso, endIsoExclusive);

        long[] bucketCounts = new long[3];

        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            double score = calculateDensityScore(m.low(), m.medium(), m.high(), m.critical(), m.durationHours());
            bucketCounts[riskLevel(score)]++;
        }

        long totalSessions = rows.size();

        List<RiskDistributionDto> out = new ArrayList<>();
        for (int level = 0; level < 3; level++) {
            double percent = totalSessions == 0
                    ? 0.0
                    : Math.round((bucketCounts[level] * 100.0) / totalSessions * 100) / 100.0; // round to 2 dp
            out.add(RiskDistributionDto.builder()
                    .id(level)
                    .value((long) percent) //This is the percent of risk level, not the count of sessions
                    .build());
        }
        return out;
    }

    private int riskLevel(double score) {
        if (score > 90.0)
            return 0;
        if (score >= 70.0)
            return 1;
        return 2;
    }

    private List<RecentSessionDto> buildRecentSessions(UUID companyId) {
        List<Object[]> rows = dashboardRepository.findRecentSessionsForCompany(
                companyId, Role.FLEET_DRIVER, PageRequest.of(0, RECENT_SESSION_LIMIT));
        List<RecentSessionDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            UUID sessionId = (UUID) row[0];
            String name = (String) row[1];
            float hours = row[2] == null ? 0f : ((Number) row[2]).floatValue();
            int alerts = row[3] == null ? 0 : ((Number) row[3]).intValue();
            list.add(RecentSessionDto.builder()
                    .driver(name)
                    .riskId(sessionRiskId(sessionId))
                    .duration(formatDuration(hours))
                    .alerts(alerts)
                    .build());
        }
        return list;
    }

    private int sessionRiskId(UUID sessionId) {
        List<Object[]> rows = dashboardRepository.findSingleSessionSafetyMetric(sessionId);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        DensityMetric m = parseMetricRow(rows.get(0));
        double score = calculateDensityScore(m.low(), m.medium(), m.high(), m.critical(), m.durationHours());
        return riskLevel(score);
    }

    private String formatDuration(float durationHours) {
        int totalMinutes = Math.round(durationHours * 60);
        if (totalMinutes <= 0) {
            return "0 mins";
        }
        return totalMinutes + " mins";
    }

    private List<TopPerformerDto> buildTopPerformers(UUID companyId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = dashboardRepository.findDriverSafetyDensityMetrics(
                companyId, Role.FLEET_DRIVER.name(), startIso, endIsoExclusive);

        if (rows.isEmpty())
            return List.of();

        Map<UUID, Long> sessionsByDriver = sessionsByDriver(companyId, startIso, endIsoExclusive);

        record EvaluatedDriver(UUID userId, double score, double hoursDriven) {
        }

        List<EvaluatedDriver> rankings = new ArrayList<>();
        for (Object[] row : rows) {
            DensityMetric m = parseMetricRow(row);
            int score = (int) Math
                    .round(calculateDensityScore(m.low(), m.medium(), m.high(), m.critical(), m.durationHours()));
            rankings.add(new EvaluatedDriver(m.entityId(), score, m.durationHours()));
        }

        List<EvaluatedDriver> topDrivers = rankings.stream()
                .sorted(Comparator.comparingDouble(EvaluatedDriver::score).reversed()
                        .thenComparing(Comparator.comparingDouble(EvaluatedDriver::hoursDriven).reversed()))
                .limit(TOP_PERFORMER_LIMIT)
                .toList();

        List<UUID> topUserIds = topDrivers.stream().map(EvaluatedDriver::userId).toList();
        Map<UUID, String> names = loadDriverNames(topUserIds);

        List<TopPerformerDto> out = new ArrayList<>();
        int rank = 1;
        for (EvaluatedDriver ed : topDrivers) {
            out.add(TopPerformerDto.builder()
                    .name(names.getOrDefault(ed.userId(), "Unknown Driver"))
                    .sessions(sessionsByDriver.getOrDefault(ed.userId(), 0L))
                    .score(ed.score())
                    .rank(rank++)
                    .build());
        }
        return out;
    }

    private Map<UUID, Long> sessionsByDriver(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : dashboardRepository.countSessionsByDriver(
                companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive)) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<UUID, String> loadDriverNames(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> map = new HashMap<>();
        for (Object[] row : dashboardRepository.findDriverNamesByIds(userIds)) {
            map.put((UUID) row[0], row[1] != null ? (String) row[1] : "");
        }
        return map;
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

        if ("5".equals(filterId)) {
            YearMonth current = YearMonth.from(from);
            YearMonth end = YearMonth.from(to);
            DateTimeFormatter shortMonth = DateTimeFormatter.ofPattern("MMM yy", Locale.US);
            while (!current.isAfter(end)) {
                sqlKeys.add(current.toString());
                displayLabels.add(current.atDay(1).format(shortMonth));
                current = current.plusMonths(1);
            }
            return new TrendResolution(TrendGranularity.MONTHLY, sqlKeys, displayLabels);
        }

        if (days <= 35) {
            DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US);
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                sqlKeys.add(d.toString());
                displayLabels.add(d.format(shortDate));
            }
            return new TrendResolution(TrendGranularity.DAILY, sqlKeys, displayLabels);
        } else {
            YearMonth current = YearMonth.from(from);
            YearMonth end = YearMonth.from(to);
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
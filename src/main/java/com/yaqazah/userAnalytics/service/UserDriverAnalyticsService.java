package com.yaqazah.userAnalytics.service;

import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.userAnalytics.model.UserAnalyticsResponseDto;
import com.yaqazah.userAnalytics.repository.UserAnalyticsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class UserDriverAnalyticsService  {

    // ==========================================
    // SAFETY SCORE ALGORITHM CONFIGURATION
    // ==========================================
    private static final double K_FACTOR = 0.025;
    private static final double MIN_TRIP_HOURS = 0.0833; // 5-minute threshold

    public enum TrendGranularity { HOURLY, DAILY, MONTHLY, YEARLY }
    private record TrendResolution(TrendGranularity granularity, List<String> sqlKeys, List<String> displayLabels) {}

    private record DensityMetric(
            UUID entityId, double durationHours,
            long low, long medium, long high, long critical
    ) {}

    private record TrendBucketMetric(
            String bucketKey, double durationHours,
            long low, long med, long high, long crit
    ) {}

    private final UserAnalyticsRepository repository;

    public UserDriverAnalyticsService(UserAnalyticsRepository repository) {
        this.repository = repository;
    }


    @Cacheable(value = "user:analytics", key = "#userId + ':' + #filter + ':' + #fromIso + ':' + #toIso")
    @Transactional(readOnly = true)
    public UserAnalyticsResponseDto buildAnalytics(UUID userId, String filter, String fromIso, String toIso) {
        DashboardFilterResolver.DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        TrendResolution resolution = resolveTrendBuckets(filter, range.from(), range.to());

        // 1. Fetch Master Session Pools (Re-uses the exact repo query from the Session service!)
        List<DensityMetric> curSessions = fetchUserSessionsDensity(userId, curStartIso, curEndExcl);
        List<DensityMetric> prevSessions = fetchUserSessionsDensity(userId, prevStartIso, prevEndExcl);

        // 2. Derive Counts & Averages purely in-memory from the pools
        long sessionsCur = curSessions.size();
        long sessionsPrev = prevSessions.size();
        Double avgScoreCur = calculatePooledUserScore(curSessions);
        Double avgScorePrev = calculatePooledUserScore(prevSessions);

        long alertsCur = repository.countTotalAlertsForUser(userId, curStartIso, curEndExcl);
        long alertsPrev = repository.countTotalAlertsForUser(userId, prevStartIso, prevEndExcl);

        // 3. Derive Risk Distributions purely in-memory from the pools
        long[] curRiskBuckets = calculateRiskBucketsFromPool(curSessions);
        long[] prevRiskBuckets = calculateRiskBucketsFromPool(prevSessions);

        List<OverviewStatDto> overviewStats = List.of(
                overviewStat("Total Sessions", (double) sessionsCur, (double) sessionsPrev, false),
                overviewStat("Safety Score", avgScoreCur, avgScorePrev, true),
                overviewStat("Total High Risk Sessions", (double) curRiskBuckets[2], (double) prevRiskBuckets[2], false)
        );

        List<PieDistributionDto> pieDistribution = buildPieDistribution(userId, curStartIso, curEndExcl);
        List<RiskDistributionDto> riskDistribution = buildRiskDistribution(curRiskBuckets);

        List<Integer> performanceTrend = buildPerformanceTrend(userId, curStartIso, curEndExcl, resolution);
        List<AlertTrendValueDto> alertTrendValues = buildUserAlertTrendValues(userId, curStartIso, curEndExcl, resolution);

        return UserAnalyticsResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(overviewStats)
                .performanceTrend(performanceTrend)
                .alertTrendValues(alertTrendValues)
                .pieDistribution(pieDistribution)
                .riskDistribution(riskDistribution)
                .trendLabels(resolution.displayLabels())
                .build();
    }

    // =========================================================================
    // DENSITY MATH & POOLING HELPERS
    // =========================================================================

    private double calculateDensityScore(long low, long med, long high, long crit, double durationHours) {
        double effectiveHours = Math.max(durationHours, MIN_TRIP_HOURS);
        long penaltyPoints = (low * 1L) + (med * 3L) + (high * 8L) + (crit * 20L);
        double eventDensity = penaltyPoints / effectiveHours;
        double score = 100.0 * Math.exp(-K_FACTOR * eventDensity);
        return Math.min(100.0, Math.max(0.0, score));
    }

    private List<DensityMetric> fetchUserSessionsDensity(UUID userId, String startIso, String endIsoExclusive) {
        List<Object[]> rows = repository.findBulkUserSessionsDensityMetrics(userId, startIso, endIsoExclusive);
        List<DensityMetric> list = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            list.add(new DensityMetric(
                    UUID.fromString((String) row[0]),
                    row[1] == null ? 0.0 : ((Number) row[1]).doubleValue(),
                    row[2] == null ? 0L : ((Number) row[2]).longValue(),
                    row[3] == null ? 0L : ((Number) row[3]).longValue(),
                    row[4] == null ? 0L : ((Number) row[4]).longValue(),
                    row[5] == null ? 0L : ((Number) row[5]).longValue()
            ));
        }
        return list;
    }

    private Double calculatePooledUserScore(List<DensityMetric> sessionMetrics) {
        if (sessionMetrics.isEmpty()) return null; // Return null if empty

        double pooledHours = 0.0;
        long low = 0, med = 0, high = 0, crit = 0;

        for (DensityMetric m : sessionMetrics) {
            pooledHours += m.durationHours();
            low += m.low();
            med += m.medium();
            high += m.high();
            crit += m.critical();
        }

        if (pooledHours <= 0) return 100.0;
        return calculateDensityScore(low, med, high, crit, pooledHours);
    }

    private long[] calculateRiskBucketsFromPool(List<DensityMetric> pool) {
        long[] buckets = new long[3];
        for (DensityMetric m : pool) {
            double score = calculateDensityScore(m.low(), m.medium(), m.high(), m.critical(), m.durationHours());
            buckets[riskLevel(score)]++;
        }
        return buckets;
    }

    private int riskLevel(double score) {
        if (score > 90.0) return 0;
        if (score >= 70.0) return 1;
        return 2;
    }

    // =========================================================================
    // DYNAMIC SYNCHRONIZED TRENDS
    // =========================================================================

    private TrendBucketMetric parseTrendRow(Object[] row) {
        return new TrendBucketMetric(
                (String) row[0],
                row[1] == null ? 0.0 : ((Number) row[1]).doubleValue(),
                row[2] == null ? 0L : ((Number) row[2]).longValue(),
                row[3] == null ? 0L : ((Number) row[3]).longValue(),
                row[4] == null ? 0L : ((Number) row[4]).longValue(),
                row[5] == null ? 0L : ((Number) row[5]).longValue()
        );
    }

    private List<Integer> buildPerformanceTrend(UUID userId, String startIso, String endIsoExclusive, TrendResolution resolution) {
        List<Object[]> rows = switch (resolution.granularity()) {
            case HOURLY -> repository.findSafetyTrendHourlyForUser(userId, startIso, endIsoExclusive);
            case DAILY -> repository.findSafetyTrendDailyForUser(userId, startIso, endIsoExclusive);
            case MONTHLY, YEARLY -> repository.findSafetyTrendMonthlyForUser(userId, startIso, endIsoExclusive);
        };

        Map<String, TrendBucketMetric> metricMap = new HashMap<>();
        if (resolution.granularity() == TrendGranularity.YEARLY) {
            Map<String, List<TrendBucketMetric>> groupedByYear = new HashMap<>();
            for (Object[] row : rows) {
                TrendBucketMetric m = parseTrendRow(row);
                if (m.bucketKey() != null && m.bucketKey().length() >= 4) {
                    String yearKey = m.bucketKey().substring(0, 4);
                    groupedByYear.computeIfAbsent(yearKey, k -> new ArrayList<>()).add(m);
                }
            }
            for (Map.Entry<String, List<TrendBucketMetric>> entry : groupedByYear.entrySet()) {
                String year = entry.getKey();
                List<TrendBucketMetric> list = entry.getValue();
                double totalDuration = 0.0;
                long low = 0, med = 0, high = 0, crit = 0;
                for (TrendBucketMetric m : list) {
                    totalDuration += m.durationHours();
                    low += m.low();
                    med += m.med();
                    high += m.high();
                    crit += m.crit();
                }
                metricMap.put(year, new TrendBucketMetric(year, totalDuration, low, med, high, crit));
            }
        } else {
            for (Object[] row : rows) {
                TrendBucketMetric m = parseTrendRow(row);
                metricMap.put(m.bucketKey(), m);
            }
        }

        List<Integer> trend = new ArrayList<>(resolution.sqlKeys().size());
        for (String expectedKey : resolution.sqlKeys()) {
            TrendBucketMetric m = metricMap.get(expectedKey);
            if (m == null || m.durationHours() <= 0) {
                trend.add(100); // Fixed Tuesday Death-Spike bug
            } else {
                double raw = calculateDensityScore(m.low(), m.med(), m.high(), m.crit(), m.durationHours());
                trend.add((int) Math.round(raw));
            }
        }
        return trend;
    }

    private List<AlertTrendValueDto> buildUserAlertTrendValues(UUID userId, String startIso, String endIsoExclusive, TrendResolution resolution) {
        List<Object[]> rows = switch (resolution.granularity()) {
            case HOURLY -> repository.countAlertsAndTypeHourlyForUser(userId, startIso, endIsoExclusive);
            case DAILY -> repository.countAlertsAndTypeDailyForUser(userId, startIso, endIsoExclusive);
            case MONTHLY, YEARLY -> repository.countAlertsAndTypeMonthlyForUser(userId, startIso, endIsoExclusive);
        };

        Map<String, Map<Integer, Long>> countMap = new HashMap<>();
        for (Object[] row : rows) {
            String bucketKey = (String) row[0];
            if (resolution.granularity() == TrendGranularity.YEARLY && bucketKey != null && bucketKey.length() >= 4) {
                bucketKey = bucketKey.substring(0, 4);
            }
            int alertId = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();

            // Trend types: 0=Asleep, 1=Drowsy, 2=Distracted
            if (alertId < 0 || alertId > 2) continue;
            countMap.computeIfAbsent(bucketKey, k -> new HashMap<>()).merge(alertId, count, Long::sum);
        }

        long[] typeTotals = new long[3];
        List<AlertTrendValueDto> out = new ArrayList<>();

        for (int typeId = 0; typeId <= 2; typeId++) {
            List<Long> paddedValues = new ArrayList<>(resolution.sqlKeys().size());
            for (String expectedKey : resolution.sqlKeys()) {
                long val = countMap.getOrDefault(expectedKey, Collections.emptyMap()).getOrDefault(typeId, 0L);
                paddedValues.add(val);
                typeTotals[typeId] += val;
            }
            out.add(AlertTrendValueDto.builder().id(typeId).values(paddedValues).percent(0).build());
        }

        long grandTotal = typeTotals[0] + typeTotals[1] + typeTotals[2];
        for (int i = 0; i < out.size(); i++) {
            int percent = grandTotal == 0 ? 0 : (int) Math.round((typeTotals[i] * 100.0) / grandTotal);
            out.get(i).setPercent(percent);
        }
        return out;
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

        if ("3".equals(filterId) || "4".equals(filterId) || days <= 35) {
            DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US);
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                sqlKeys.add(d.toString());
                displayLabels.add("3".equals(filterId) ? d.getDayOfWeek().name().substring(0, 3) :
                        ("4".equals(filterId) ? String.valueOf(d.getDayOfMonth()) : d.format(shortDate)));
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

    // =========================================================================
    // DTO & UI FORMATTING HELPERS
    // =========================================================================

    private List<RiskDistributionDto> buildRiskDistribution(long[] riskBuckets) {
        long total = riskBuckets[0] + riskBuckets[1] + riskBuckets[2];

        long val0 = total == 0 ? 0 : Math.round((riskBuckets[0] * 100.0) / total);
        long val1 = total == 0 ? 0 : Math.round((riskBuckets[1] * 100.0) / total);
        long val2 = total == 0 ? 0 : Math.round((riskBuckets[2] * 100.0) / total);

        return List.of(
                RiskDistributionDto.builder().id(0).value(val0).build(),
                RiskDistributionDto.builder().id(1).value(val1).build(),
                RiskDistributionDto.builder().id(2).value(val2).build()
        );
    }

    private List<PieDistributionDto> buildPieDistribution(UUID userId, String startIso, String endIsoExclusive) {
        long[] countsById = new long[6];
        long totalAlerts = 0;

        for (Object[] row : repository.countAlertsByTypeForUserInPeriod(userId, startIso, endIsoExclusive)) {
            if (row[0] == null || row[1] == null) continue;

            int alertId = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();

            if (alertId >= 3 && alertId <= 5) {
                countsById[alertId] += count;
                totalAlerts += count;
            }
        }

        if (totalAlerts == 0) return Collections.emptyList();

        List<PieDistributionDto> slices = new ArrayList<>();
        for (int id = 3; id <= 5; id++) {
            long count = countsById[id];
            if (count == 0) continue;

            int percent = (int) Math.round((count * 100.0) / totalAlerts);
            slices.add(PieDistributionDto.builder().id(id).percent(percent).build());
        }
        return slices;
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent) {
        return overviewStat(label, current, previous, asPercent, false);
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent, boolean asTime) {
        String valueStr = (current == null) ? "N/A" : (asTime ? formatDrivingHours(current) : (asPercent ? Math.round(current) + "%" : String.valueOf(Math.round(current))));
        String deltaStr = (current == null || previous == null) ? "N/A" : formatDeltaPercent(deltaPercent(current, previous));

        return OverviewStatDto.builder()
                .label(label)
                .value(valueStr)
                .delta(deltaStr)
                .build();
    }

    private String formatDrivingHours(double hours) {
        int totalSeconds = (int) Math.round(hours * 3600);
        return String.format(Locale.US, "%d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    private Double deltaPercent(double current, double previous) {
        return (previous == 0.0) ? (current == 0.0 ? 0.0 : 100.0) : ((current - previous) / previous) * 100.0;
    }

    private String formatDeltaPercent(double delta) {
        if (delta == 0.0) return "0%";
        String sign = delta > 0 ? "+" : "";
        return (Math.abs(delta - Math.round(delta)) < 0.05) ? sign + Math.round(delta) + "%" : sign + String.format(Locale.US, "%.1f", delta) + "%";
    }

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate pEnd = fromDate.minusDays(1);
        return new LocalDate[]{pEnd.minusDays(days - 1), pEnd};
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }
}
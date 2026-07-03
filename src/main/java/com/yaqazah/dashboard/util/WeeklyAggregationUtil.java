package com.yaqazah.dashboard.util;

import com.yaqazah.dashboard.dto.AlertTrendValueDto;

import java.util.ArrayList;
import java.util.List;

public final class WeeklyAggregationUtil {

    private WeeklyAggregationUtil() {
        // Hide utility class constructor
    }

    public record WeeklyAggregationResult(
            List<String> trendLabels,
            List<Integer> performanceTrend,
            List<AlertTrendValueDto> alertTrendValues
    ) {}

    public static WeeklyAggregationResult aggregateMonthToWeeks(
            List<String> dailyLabels,
            List<Integer> performanceTrend,
            List<AlertTrendValueDto> alertTrendValues) {

        if (dailyLabels == null || alertTrendValues == null) {
            return new WeeklyAggregationResult(dailyLabels, performanceTrend, alertTrendValues);
        }

        int numDays = dailyLabels.size();
        if (numDays == 0) {
            return new WeeklyAggregationResult(dailyLabels, performanceTrend, alertTrendValues);
        }

        List<String> weeklyLabels = new ArrayList<>();
        List<Integer> weeklyPerformance = performanceTrend != null ? new ArrayList<>() : null;
        
        int numAlertTypes = alertTrendValues.size();
        List<List<Long>> newAlertValues = new ArrayList<>(numAlertTypes);
        for (int i = 0; i < numAlertTypes; i++) {
            newAlertValues.add(new ArrayList<>());
        }

        for (int start = 0; start < numDays; start += 7) {
            int end = Math.min(start + 7, numDays);
            
            // 1. Label: "Week 1", "Week 2", etc.
            weeklyLabels.add("Week " + ((start / 7) + 1));

            // 2. Performance Trend (Mathematical Average)
            if (performanceTrend != null) {
                double sumPerf = 0;
                int perfCount = 0;
                for (int i = start; i < end; i++) {
                    if (i < performanceTrend.size() && performanceTrend.get(i) != null) {
                        sumPerf += performanceTrend.get(i);
                        perfCount++;
                    }
                }
                weeklyPerformance.add(perfCount > 0 ? (int) Math.round(sumPerf / perfCount) : 0);
            }

            // 3. Alert Trend Values (Sum)
            for (int typeIndex = 0; typeIndex < numAlertTypes; typeIndex++) {
                long sumAlerts = 0;
                List<Long> oldVals = alertTrendValues.get(typeIndex).getValues();
                if (oldVals != null) {
                    for (int i = start; i < end; i++) {
                        if (i < oldVals.size() && oldVals.get(i) != null) {
                            sumAlerts += oldVals.get(i);
                        }
                    }
                }
                newAlertValues.get(typeIndex).add(sumAlerts);
            }
        }

        List<AlertTrendValueDto> weeklyAlertTrends = new ArrayList<>(numAlertTypes);
        for (int typeIndex = 0; typeIndex < numAlertTypes; typeIndex++) {
            AlertTrendValueDto oldDto = alertTrendValues.get(typeIndex);
            weeklyAlertTrends.add(AlertTrendValueDto.builder()
                    .id(oldDto.getId())
                    .percent(oldDto.getPercent())
                    .values(newAlertValues.get(typeIndex))
                    .build());
        }

        return new WeeklyAggregationResult(weeklyLabels, weeklyPerformance, weeklyAlertTrends);
    }
}

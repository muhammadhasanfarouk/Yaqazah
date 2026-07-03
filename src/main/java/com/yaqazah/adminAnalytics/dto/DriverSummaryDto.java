package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryDto {
    private String name;
    private String id;
    private String email;
    //    private String phone;
    private int riskId;
    private int safetyScore;
    private long totalSessions;
    private String lastSession;
    private String joinedAt;
    private String status;
}

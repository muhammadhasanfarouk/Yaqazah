package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DetectionLogDto {
    private String eventId;
    private String timestamp;
    private int alertId;
    private int riskId;
    private String title;
    private String subtitle;
    private String snapshotUrl;
}

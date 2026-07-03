package com.yaqazah.detection.controller;

import com.yaqazah.adminAnalytics.dto.DetectionLogDto;
import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.repository.DetectionLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * DetectionController: legacy read-only endpoint for fetching detection logs by session.
 * Writing detection logs is now done via POST /api/sessions.
 */
@RestController
@RequestMapping("/api/detections")
@CrossOrigin(origins = "*")
@Tag(name = "Detections", description = "Read-only detection log endpoints.")
@RequiredArgsConstructor
@NullMarked
public class DetectionController {

    private final DetectionLogRepository detectionLogRepo;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<DetectionLogDto>> getSessionLogs(@PathVariable UUID sessionId) {
        List<DetectionLog> logs = detectionLogRepo.findBySession_SessionId(sessionId);
        List<DetectionLogDto> dtos = logs.stream().map(log -> DetectionLogDto.builder()
                .eventId(log.getEventId() != null ? log.getEventId().toString() : null)
                .timestamp(log.getTimestamp())
                .alertId(log.getAlertId())
                .riskId(log.getRiskId())
                .title(log.getTitle())
                .subtitle(log.getSubtitle())
                .snapshotUrl(log.getSnapshotUrl())
                .build()
        ).toList();
        return ResponseEntity.ok(dtos);
    }
}
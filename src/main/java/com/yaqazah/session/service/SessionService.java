package com.yaqazah.session.service;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.infrastructure.storage.service.FileService;
import com.yaqazah.session.dto.LogPayload;
import com.yaqazah.session.dto.SessionUploadRequest;
import com.yaqazah.session.model.Session;
import com.yaqazah.session.repository.SessionRepository;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "admin:sessions", allEntries = true),
            @CacheEvict(value = "admin:session-detail", allEntries = true),
            @CacheEvict(value = "admin:drivers", allEntries = true),
            @CacheEvict(value = "admin:driver-detail", allEntries = true),
            @CacheEvict(value = "user:analytics", allEntries = true),
            @CacheEvict(value = "user:sessions", allEntries = true),
            @CacheEvict(value = "user:session-detail", allEntries = true)
    })
    @Transactional
    public Session uploadSession(SessionUploadRequest request, String authenticatedUserEmail) {
        // 1. Resolve the authenticated user from their email
        User authUser = userRepository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + authenticatedUserEmail));

        // 2. Resolve the target user: use payload's userId if provided, otherwise default to authenticated user
        UUID targetUserId = request.getSession().getUserId();
        User user;

        if (targetUserId != null) {
            // SECURITY CHECK: Drivers cannot upload sessions on behalf of other drivers
            if (!authUser.getUserId().equals(targetUserId) && authUser.getRole() != Role.ADMIN) {
                throw new SecurityException("Access denied: You cannot upload sessions for another driver.");
            }
            user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + targetUserId));
        } else {
            user = authUser;
        }

        UUID userId = user.getUserId();

        // 2. Compute duration: use frontend-provided value (double hours)
        double durationHours = (request.getSession().getDuration()) / 3600;

        // 3. Count total alerts: logs where alertId >= 0 (exclude session start/end events with alertId = -1)
        List<LogPayload> logs = request.getLogs() != null ? request.getLogs() : new ArrayList<>();
        int totalAlerts = (int) logs.stream().filter(l -> l.getAlertId() >= 0).count();

        // 4. Build and save the Session
        Session session = new Session();
        session.setUserId(userId);
        session.setStartDateTime(request.getSession().getStartDateTime());
        session.setEndDateTime(request.getSession().getEndDateTime());
        session.setDurationHours(durationHours);
        session.setTotalAlerts(totalAlerts);
        session.setInsertionTimestamp(Instant.now().toString());

        Session savedSession = sessionRepository.save(session);

        // 5. Build and save all detection logs
        List<DetectionLog> detectionLogs = new ArrayList<>();
        for (LogPayload logPayload : logs) {
            DetectionLog log = new DetectionLog();
            log.setSession(savedSession);
            log.setUser(user);
            log.setTitle(logPayload.getTitle());
            log.setSubtitle(logPayload.getSubtitle());
            log.setTimestamp(logPayload.getTimestamp());
            log.setAlertId(logPayload.getAlertId());
            log.setRiskId(logPayload.getRiskId());

            // Handle image upload if a base64 string is provided, otherwise use the snapshotUrl directly
            String snapshotUrl = logPayload.getSnapshotUrl();
            if (logPayload.getPhotoBase64() != null && !logPayload.getPhotoBase64().trim().isEmpty()) {
                String fileName = "event_" + user.getUserId() + "_" + UUID.randomUUID() + ".jpg";
                snapshotUrl = fileService.uploadBase64(logPayload.getPhotoBase64(), fileName);
            }
            log.setSnapshotUrl(snapshotUrl);

            log.setInsertionTimestamp(Instant.now().toString());
            detectionLogs.add(log);
        }
        detectionLogRepository.saveAll(detectionLogs);

        return savedSession;
    }

    public List<Session> getSessions(UUID driverId) {
        return sessionRepository.findByUserId(driverId);
    }

    public Session getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }
}
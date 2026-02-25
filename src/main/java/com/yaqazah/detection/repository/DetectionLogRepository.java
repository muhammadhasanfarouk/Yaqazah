package com.yaqazah.detection.repository;

import com.yaqazah.detection.model.DetectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionLogRepository extends JpaRepository<DetectionLog, UUID> {
    List<DetectionLog> findBySessionId(UUID sessionId);

    List<DetectionLog> findByTimestampStartingWith(String date);
}
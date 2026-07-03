package com.yaqazah.detection.repository;

import com.yaqazah.detection.model.DetectionLog;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@NullMarked
public interface DetectionLogRepository extends JpaRepository<DetectionLog, UUID> {
    List<DetectionLog> findBySession_SessionId(UUID sessionSessionId);

    List<DetectionLog> findByTimestampStartingWith(String date);

    void deleteByUser(com.yaqazah.user.model.User user);
}
package com.yaqazah.notification.repository;

import com.yaqazah.notification.model.AlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<AlertConfig, UUID> {
    Optional<AlertConfig> findByUserId(UUID driverId);
}
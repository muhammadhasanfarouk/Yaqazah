package com.yaqazah.session.repository;

import com.yaqazah.session.model.Session;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@NullMarked

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByUserId(UUID driverId);

    List<Session> findByStartDateTimeStartingWith(String date);

    void deleteByUserId(UUID userId);
}
package com.yaqazah.dashboard.repository;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.user.model.Role;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics queries for the dashboard. Uses JPQL with string bounds
 * for ISO timestamps.
 */
@NullMarked
public interface DashboardRepository extends JpaRepository<DetectionLog, UUID> {

    @Query("""
            select count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            """)
    long countTotalAlerts(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(s)
            from Session s
            join User u on s.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            """)
    long countSessions(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.userId, count(s)
            from Session s
            join User u on s.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            group by s.userId
            """)
    List<Object[]> countSessionsByDriver(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 13), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 13), d.alertId
            """)
    List<Object[]> countAlertsHourly(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 10), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 10), d.alertId
            """)
    List<Object[]> countAlertsDaily(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 7), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 7), d.alertId
            """)
    List<Object[]> countAlertsMonthly(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId between 3 and 5
            group by d.alertId
            """)
    List<Object[]> countAlertsByType(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.sessionId, u.fullName, s.durationHours, s.totalAlerts
            from Session s
            join User u on s.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
            order by s.startDateTime desc
            """)
    List<Object[]> findRecentSessionsForCompany(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            Pageable pageable);

    @Query("""
            select u.userId, u.fullName
            from User u
            where u.userId in :userIds
            """)
    List<Object[]> findDriverNamesByIds(@Param("userIds") List<UUID> userIds);

    // 2. Count distinct active drivers in a specific time period
    @Query("""
            select count(distinct s.userId)
            from Session s
            join User u on s.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :role
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIso
            """)
    long countActiveDrivers(
            @Param("companyId") UUID companyId,
            @Param("role") Role role,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 1. Fetches Driver aggregation (Total Hours + counts of all 4 risk levels)
    @Query(value = """
            SELECT
                CAST(u.user_id AS varchar) AS entity_id,
                COALESCE(ds.total_hours, 0) AS duration_hours,
                COALESCE(dl.low_count, 0) AS low_count,
                COALESCE(dl.med_count, 0) AS med_count,
                COALESCE(dl.high_count, 0) AS high_count,
                COALESCE(dl.crit_count, 0) AS crit_count
            FROM users u
            JOIN (
                SELECT user_id, SUM(duration_hours) AS total_hours
                FROM sessions
                WHERE start_date_time >= :startIso
                  AND start_date_time < :endIso
                GROUP BY user_id
            ) ds ON u.user_id = ds.user_id
            LEFT JOIN (
                SELECT s.user_id,
                       COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                       COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                       COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                       COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
                FROM sessions s
                JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
                WHERE s.start_date_time >= :startIso
                  AND s.start_date_time < :endIso
                GROUP BY s.user_id
            ) dl ON u.user_id = dl.user_id
            WHERE u.company_id = :companyId
              AND u.role = :role
            """, nativeQuery = true)
    List<Object[]> findDriverSafetyDensityMetrics(
            @Param("companyId") UUID companyId,
            @Param("role") String role,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 3. Fetches Session aggregation (Total Hours + counts of all 4 risk levels)
    @Query(value = """
            SELECT
                CAST(s.session_id AS varchar) AS entity_id,
                COALESCE(s.duration_hours, 0) AS duration_hours,
                COALESCE(dl.low_count, 0) AS low_count,
                COALESCE(dl.med_count, 0) AS med_count,
                COALESCE(dl.high_count, 0) AS high_count,
                COALESCE(dl.crit_count, 0) AS crit_count
            FROM sessions s
            JOIN users u ON s.user_id = u.user_id
            LEFT JOIN (
                SELECT de.session_id,
                       COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                       COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                       COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                       COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
                FROM detection_log de
                WHERE de.alert_id >= 0
                GROUP BY de.session_id
            ) dl ON s.session_id = dl.session_id
            WHERE u.company_id = :companyId
              AND u.role = :role
              AND s.start_date_time >= :startIso
              AND s.start_date_time < :endIso
            """, nativeQuery = true)
    List<Object[]> findSessionSafetyDensityMetrics(
            @Param("companyId") UUID companyId,
            @Param("role") String role,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 2. Fetches a single Session's aggregation
    @Query(value = """
            SELECT
                CAST(s.session_id AS varchar) AS entity_id,
                COALESCE(s.duration_hours, 0) AS duration_hours,
                COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
            FROM sessions s
            LEFT JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
            WHERE s.session_id = :sessionId
            GROUP BY s.session_id, s.duration_hours
            """, nativeQuery = true)
    List<Object[]> findSingleSessionSafetyMetric(@Param("sessionId") UUID sessionId);
}

package com.yaqazah.userAnalytics.repository;

import com.yaqazah.detection.model.DetectionLog;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface UserAnalyticsRepository extends JpaRepository<DetectionLog, UUID> {

    @Query("""
            select count(s)
            from Session s
            join User u on s.userId = u.userId
            where u.userId = :userId
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            """)
    long countSessionsForUser(
            @Param("userId") UUID userId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(d)
            from DetectionLog d
            join d.session s
            join User u on s.userId = u.userId
            where u.userId = :userId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            """)
    long countTotalAlertsForUser(
            @Param("userId") UUID userId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.sessionId, s.startDateTime, s.durationHours, s.totalAlerts, u.userId, u.fullName
            from Session s
            join User u on s.userId = u.userId
            where u.userId = :userId
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            order by s.startDateTime desc
            """)
    List<Object[]> findSessionsForUserInPeriod(
            @Param("userId") UUID userId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.sessionId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts
            from Session s
            join User u on s.userId = u.userId
            where s.sessionId = :sessionId
              and u.userId = :userId
            """)
    Optional<Object> findSessionForUser(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId);

    @Query("""
            select d.eventId, d.timestamp, d.alertId, d.riskId, d.title, d.subtitle, d.snapshotUrl
            from DetectionLog d
            join d.session s
            join User u on s.userId = u.userId
            where d.session.sessionId = :sessionId
            order by d.timestamp asc
            """)
    List<Object[]> findLogsForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select d.riskId, count(d)
            from DetectionLog d
            join d.session s
            join User u on s.userId = u.userId
            where d.session.sessionId = :sessionId
              and d.riskId >= 0
            group by d.riskId
            """)
    List<Object[]> countAlertsByTypeForSession(@Param("sessionId") UUID sessionId);


    // 2. For the Pie Chart
    @Query("""
            select d.alertId, count(d)
            from DetectionLog d
            join d.session s
            where s.userId = :userId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId between 3 and 5
            group by d.alertId
            """)
    List<Object[]> countAlertsByTypeForUserInPeriod(
            @Param("userId") UUID userId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 13), d.alertId, count(d)
            from DetectionLog d
            where d.user.userId = :userId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 13), d.alertId
            """)
    List<Object[]> countAlertsAndTypeHourlyForUser(
            @Param("userId") UUID userId, @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 10), d.alertId, count(d)
            from DetectionLog d
            where d.user.userId = :userId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 10), d.alertId
            """)
    List<Object[]> countAlertsAndTypeDailyForUser(
            @Param("userId") UUID userId, @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 7), d.alertId, count(d)
            from DetectionLog d
            where d.user.userId = :userId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 7), d.alertId
            """)
    List<Object[]> countAlertsAndTypeMonthlyForUser(
            @Param("userId") UUID userId, @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    // 1. Bulk metrics for an individual driver's session list
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
            WHERE s.user_id = :userId
              AND s.start_date_time >= :startIso
              AND s.start_date_time < :endIso
            GROUP BY s.session_id, s.duration_hours
            """, nativeQuery = true)
    List<Object[]> findBulkUserSessionsDensityMetrics(
            @Param("userId") UUID userId,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 2. Single metric lookup for the Driver's "Session Details" view
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

    @Query(value = """
            SELECT
                ds.bucket_key,
                COALESCE(ds.duration_hours, 0) AS duration_hours,
                COALESCE(dl.low_count, 0) AS low_count,
                COALESCE(dl.med_count, 0) AS med_count,
                COALESCE(dl.high_count, 0) AS high_count,
                COALESCE(dl.crit_count, 0) AS crit_count
            FROM (
                SELECT
                    TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM-DD"T"HH24') AS bucket_key,
                    SUM(COALESCE(duration_hours, 0)) AS duration_hours
                FROM sessions
                WHERE user_id = :userId AND start_date_time >= :startIso AND start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM-DD"T"HH24')
            ) ds
            LEFT JOIN (
                SELECT
                    TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM-DD"T"HH24') AS bucket_key,
                    COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                    COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                    COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                    COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
                FROM sessions s
                JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
                WHERE s.user_id = :userId AND s.start_date_time >= :startIso AND s.start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM-DD"T"HH24')
            ) dl ON ds.bucket_key = dl.bucket_key
            """, nativeQuery = true)
    List<Object[]> findSafetyTrendHourlyForUser(@Param("userId") UUID userId, @Param("startIso") String startIso,
                                                @Param("endIso") String endIso);

    @Query(value = """
            SELECT
                ds.bucket_key,
                COALESCE(ds.duration_hours, 0) AS duration_hours,
                COALESCE(dl.low_count, 0) AS low_count,
                COALESCE(dl.med_count, 0) AS med_count,
                COALESCE(dl.high_count, 0) AS high_count,
                COALESCE(dl.crit_count, 0) AS crit_count
            FROM (
                SELECT
                    TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM-DD') AS bucket_key,
                    SUM(COALESCE(duration_hours, 0)) AS duration_hours
                FROM sessions
                WHERE user_id = :userId AND start_date_time >= :startIso AND start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM-DD')
            ) ds
            LEFT JOIN (
                SELECT
                    TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM-DD') AS bucket_key,
                    COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                    COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                    COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                    COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
                FROM sessions s
                JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
                WHERE s.user_id = :userId AND s.start_date_time >= :startIso AND s.start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM-DD')
            ) dl ON ds.bucket_key = dl.bucket_key
            """, nativeQuery = true)
    List<Object[]> findSafetyTrendDailyForUser(@Param("userId") UUID userId, @Param("startIso") String startIso,
                                               @Param("endIso") String endIso);

    @Query(value = """
            SELECT
                ds.bucket_key,
                COALESCE(ds.duration_hours, 0) AS duration_hours,
                COALESCE(dl.low_count, 0) AS low_count,
                COALESCE(dl.med_count, 0) AS med_count,
                COALESCE(dl.high_count, 0) AS high_count,
                COALESCE(dl.crit_count, 0) AS crit_count
            FROM (
                SELECT
                    TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM') AS bucket_key,
                    SUM(COALESCE(duration_hours, 0)) AS duration_hours
                FROM sessions
                WHERE user_id = :userId AND start_date_time >= :startIso AND start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(start_date_time AS TIMESTAMP), 'YYYY-MM')
            ) ds
            LEFT JOIN (
                SELECT
                    TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM') AS bucket_key,
                    COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                    COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                    COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                    COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
                FROM sessions s
                JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
                WHERE s.user_id = :userId AND s.start_date_time >= :startIso AND s.start_date_time < :endIso
                GROUP BY TO_CHAR(CAST(s.start_date_time AS TIMESTAMP), 'YYYY-MM')
            ) dl ON ds.bucket_key = dl.bucket_key
            """, nativeQuery = true)
    List<Object[]> findSafetyTrendMonthlyForUser(@Param("userId") UUID userId, @Param("startIso") String startIso,
                                                 @Param("endIso") String endIso);

}

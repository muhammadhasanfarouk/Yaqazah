package com.yaqazah.adminAnalytics.repository;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.user.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdminAnalyticsRepository extends JpaRepository<DetectionLog, UUID> {

    @Query("""
            select s.sessionId, u.fullName, u.userId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts,
                   sum(case when d.alertId = 2 then 1 else 0 end),
                   sum(case when d.alertId = 1 then 1 else 0 end),
                   sum(case when d.alertId = 0 then 1 else 0 end)
            from Session s
            join User u on s.userId = u.userId
            left join DetectionLog d on d.session.sessionId = s.sessionId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            group by s.sessionId, u.fullName, u.userId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts
            order by s.startDateTime desc
            """)
    List<Object[]> findSessionsForCompanyInPeriod(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.sessionId, u.fullName, u.userId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts
            from Session s
            join User u on s.userId = u.userId
            where s.sessionId = :sessionId
              and u.company.companyId = :companyId
              and u.role = :driverRole
            """)
    List<Object[]> findSessionForCompany(
            @Param("sessionId") UUID sessionId,
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole);

    @Query("""
            select d.eventId, d.timestamp, d.alertId, d.riskId, d.title, d.subtitle, d.snapshotUrl
            from DetectionLog d
            where d.session.sessionId = :sessionId
            order by d.timestamp asc
            """)
    List<Object[]> findLogsForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select d.alertId, count(d)
            from DetectionLog d
            where d.session.sessionId = :sessionId
              and d.alertId >= 0
            group by d.alertId
            """)
    List<Object[]> countAlertsByTypeForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select u.userId, u.fullName, u.email, u.status, u.insertedAt
            from User u
            where u.company.companyId = :companyId
              and u.role = :driverRole
            order by u.fullName asc
            """)
    List<Object[]> findFleetDriversForCompany(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole);

    @Query("""
            select u.userId, u.fullName, u.email, u.status, u.insertedAt
            from User u
            where u.userId = :driverId
              and u.company.companyId = :companyId
              and u.role = :driverRole
            """)
    List<Object[]> findDriverForCompany(
            @Param("driverId") UUID driverId,
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole);

    @Query("""
            select count(s)
            from Session s
            where s.userId = :driverId
            """)
    long countSessionsForDriver(@Param("driverId") UUID driverId);

    @Query("""
            select s.startDateTime
            from Session s
            where s.userId = :driverId
            order by s.startDateTime desc
            """)
    List<String> findLastSessionStartTime(@Param("driverId") UUID driverId, Pageable pageable);


    @Query("""
            select s.sessionId, s.startDateTime, s.durationHours, s.totalAlerts,
                   sum(case when d.alertId = 2 then 1 else 0 end),
                   sum(case when d.alertId = 1 then 1 else 0 end),
                   sum(case when d.alertId = 0 then 1 else 0 end)
            from Session s
            left join DetectionLog d on d.session.sessionId = s.sessionId
            where s.userId = :driverId
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            group by s.sessionId, s.startDateTime, s.durationHours, s.totalAlerts
            order by s.startDateTime desc
            """)
    List<Object[]> findSessionsForDriverInPeriod(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);


    @Query("""
            select coalesce(sum(s.durationHours), 0)
            from Session s
            where s.userId = :driverId
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            """)
    double sumDrivingHoursForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);


    @Query("""
            select count(distinct s.userId)
            from Session s
            join User u on s.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startDateTime >= :startIso
              and s.startDateTime < :endIsoExclusive
            """)
    long countActiveDrivers(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
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
            select count(d)
            from DetectionLog d
            join d.session s
            where s.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            """)
    long countAlertsForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);


    @Query("""
            select d.alertId, count(d)
            from DetectionLog d
            join d.session s
            where s.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and d.alertId >= 0
            group by d.alertId
            """)
    List<Object[]> countAlertsByTypeForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 13), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 13), d.alertId
            """)
    List<Object[]> countDriverAlertsHourly(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 10), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 10), d.alertId
            """)
    List<Object[]> countDriverAlertsDaily(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select substring(d.timestamp, 1, 7), d.alertId, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.userId = u.userId
              and d.alertId >= 0
            group by substring(d.timestamp, 1, 7), d.alertId
            """)
    List<Object[]> countDriverAlertsMonthly(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

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
                CAST(s.session_id AS varchar) AS entity_id,
                COALESCE(s.duration_hours, 0) AS duration_hours,
                COUNT(CASE WHEN de.risk_id = 0 THEN 1 END) AS low_count,
                COUNT(CASE WHEN de.risk_id = 1 THEN 1 END) AS med_count,
                COUNT(CASE WHEN de.risk_id = 2 THEN 1 END) AS high_count,
                COUNT(CASE WHEN de.risk_id >= 3 THEN 1 END) AS crit_count
            FROM sessions s
            JOIN users u ON s.user_id = u.user_id
            LEFT JOIN detection_log de ON s.session_id = de.session_id AND de.alert_id >= 0
            WHERE u.company_id = :companyId
              AND u.role = :role
              AND s.start_date_time >= :startIso
              AND s.start_date_time < :endIso
            GROUP BY s.session_id, s.duration_hours
            """, nativeQuery = true)
    List<Object[]> findBulkSessionSafetyDensityMetrics(
            @Param("companyId") UUID companyId,
            @Param("role") String role,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);


    @Query("""
            select cast(s.sessionId as string),
                   coalesce(s.durationHours, 0.0),
                   sum(case when d.riskId = 0 then 1 else 0 end),
                   sum(case when d.riskId = 1 then 1 else 0 end),
                   sum(case when d.riskId = 2 then 1 else 0 end),
                   sum(case when d.riskId = 3 then 1 else 0 end)
            from Session s
            left join DetectionLog d on d.session.sessionId = s.sessionId
            where s.sessionId in :sessionIds
            group by s.sessionId, s.durationHours
            """)
    List<Object[]> findBulkSessionSafetyDensityMetrics(@Param("sessionIds") Collection<UUID> sessionIds);

}

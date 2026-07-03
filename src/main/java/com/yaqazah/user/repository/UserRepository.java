package com.yaqazah.user.repository;

import com.yaqazah.company.model.Company;
import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@NullMarked
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    long countByRole(Role role);

    boolean existsByCompany(Company company);

    Optional<User> findFirstByRoleOrderByInsertedAtAsc(Role role);

    int countByCompany_CompanyIdAndRole(UUID companyId, Role role);

    List<User> findByCompany_CompanyIdAndRole(UUID companyId, Role role);

    void deleteByCompany_CompanyIdAndRole(UUID companyId, Role role);

    boolean existsByCompany_CompanyIdAndRoleAndIsDeletedFalse(UUID companyId, Role role);

    long countByCompany_CompanyIdAndRoleAndIsDeletedFalse(
            UUID companyId,
            Role role
    );


    int countByCompany_CompanyIdAndRoleInAndIsDeletedFalse(
            UUID companyId,
            List<Role> roles
    );


    Optional<User> findFirstByCompany_CompanyIdAndRoleAndIsDeletedFalseOrderByInsertedAtAsc(
            UUID companyId,
            Role role
    );


    List<User> findByCompany_CompanyIdAndRoleAndIsDeletedFalse(
            UUID companyId,
            Role role
    );

    @Query("""
            SELECT u
            FROM User u
            WHERE u.company.companyId = :companyId
            AND u.role = :role
            """)
    List<User> findByCompany_CompanyIdAndRoleIncludingDeleted(
            @Param("companyId") UUID companyId,
            @Param("role") Role role
    );

    List<User> findByStatusAndInsertedAtBefore(
            UserStatus status,
            Instant cutoff
    );

    boolean existsByEmail(String email);

    // --- FIX 1: Changed Optional<Long> to Optional<UUID> ---
    @Query("SELECT u.company.companyId FROM User u WHERE u.email = :email")
    Optional<UUID> findCompanyIdByEmail(@Param("email") String email);

    // --- FIX 2: Added nativeQuery to bypass the @SQLRestriction("is_deleted = false") ---
    @Query(value = "SELECT * FROM users WHERE is_deleted = true AND deleted_at < :cutoffDate", nativeQuery = true)
    List<User> findByIsDeletedTrueAndDeletedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    int countByCompany_CompanyIdAndRoleIn(UUID companyId, List<Role> roles);

    List<User> findByCompany_CompanyIdAndRoleIn(UUID companyId, List<Role> roles);

    @Query("SELECT new com.yaqazah.report.dto.DriverSessionReportDto(" +
            "u.userId, u.fullName, s.sessionId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts, " +
            "d.eventId, d.timestamp, d.alertId, d.riskId, d.title, d.subtitle) " +
            "FROM User u " +
            "LEFT JOIN Session s ON u.userId = s.userId " +
            "LEFT JOIN DetectionLog d ON s.sessionId = d.session.sessionId " +
            "WHERE u.company.companyId = :companyId AND u.role = com.yaqazah.user.model.Role.FLEET_DRIVER")
    List<DriverSessionReportDto> findCombinedDriverDataByCompany(@Param("companyId") UUID companyId);
}
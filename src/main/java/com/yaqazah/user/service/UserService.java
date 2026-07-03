package com.yaqazah.user.service;

import com.yaqazah.common.util.PasswordGeneratorUtil;
import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.session.repository.SessionRepository;
import com.yaqazah.user.dto.request.FleetDriverDto;
import com.yaqazah.user.dto.response.AdminCompanyDashboardDto;
import com.yaqazah.user.dto.response.AdminListDto;
import com.yaqazah.user.dto.response.CompanyInfoDto;
import com.yaqazah.user.dto.response.UserProfileResponseDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.RefreshTokenRepository;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    // --- REPOSITORIES & DEPENDENCIES ---
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompanyRepository companyRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public void updateUserName(String email, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }

        User user = findByEmail(email);
        user.setFullName(newName);
        userRepository.save(user);
    }

    public UserProfileResponseDto getUserProfileDto(String email) {
        User user = findByEmail(email);

        UserProfileResponseDto response = new UserProfileResponseDto();
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setGender(user.getGender().name());
        response.setRole(user.getRole().name());
        response.setBirthDate(user.getBirthDate());
        response.setInsertedAt(user.getInsertedAt());

        return response;
    }

    // ========================================================================
    // 1. SOFT DELETE (Called immediately when user clicks "Delete My Account")
    // ========================================================================
    @Transactional
    public void deleteAccount(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Soft delete the user first so counts exclude this user
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);


        switch (user.getRole()) {

            case INDEPENDENT_DRIVER:
            case FLEET_DRIVER:
                // Only delete this driver
                break;


            case ADMIN:

                Company adminCompany = user.getCompany();

                if (adminCompany == null) {
                    break;
                }

                UUID adminCompanyId = adminCompany.getCompanyId();


                // Count remaining company owners (excluding deleted)
                long remainingAdmins =
                        userRepository.countByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                                adminCompanyId,
                                Role.ADMIN
                        );


                // If this was the last owner, promote oldest company admin
                if (remainingAdmins == 0) {

                    Optional<User> oldestCompanyAdmin =
                            userRepository
                                    .findFirstByCompany_CompanyIdAndRoleAndIsDeletedFalseOrderByInsertedAtAsc(
                                            adminCompanyId,
                                            Role.COMPANY_ADMIN
                                    );


                    if (oldestCompanyAdmin.isPresent()) {

                        User newOwner = oldestCompanyAdmin.get();

                        newOwner.setRole(Role.ADMIN);

                        userRepository.save(newOwner);

                    } else {

                        // No admins left -> delete company and drivers

                        List<User> fleetDrivers =
                                userRepository.findByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                                        adminCompanyId,
                                        Role.FLEET_DRIVER
                                );


                        for (User driver : fleetDrivers) {

                            driver.setDeleted(true);
                            driver.setDeletedAt(Instant.now());

                            userRepository.save(driver);
                        }


                        adminCompany.setDeleted(true);
                        adminCompany.setDeletedAt(Instant.now());

                        companyRepository.save(adminCompany);
                    }
                }

                break;


            case COMPANY_ADMIN:

                Company company = user.getCompany();

                if (company == null) {
                    break;
                }


                UUID companyId = company.getCompanyId();


                // Remaining leaders after deleting this admin
                int remainingLeaders =
                        userRepository.countByCompany_CompanyIdAndRoleInAndIsDeletedFalse(
                                companyId,
                                List.of(
                                        Role.ADMIN,
                                        Role.COMPANY_ADMIN
                                )
                        );


                // No owner/admin left
                if (remainingLeaders == 0) {


                    List<User> fleetDrivers =
                            userRepository.findByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                                    companyId,
                                    Role.FLEET_DRIVER
                            );


                    for (User driver : fleetDrivers) {

                        driver.setDeleted(true);
                        driver.setDeletedAt(Instant.now());

                        userRepository.save(driver);
                    }


                    company.setDeleted(true);
                    company.setDeletedAt(Instant.now());

                    companyRepository.save(company);
                }


                break;


            default:
                throw new IllegalStateException(
                        "Deletion not permitted for role: " + user.getRole()
                );
        }
    }

    // ========================================================================
    // 2. HARD DELETE (Called by your Scheduled Cleanup Service after 30 days)
    // ========================================================================
    @Transactional
    public void hardDeleteAccount(UUID userId) {

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) return;


        switch (user.getRole()) {


            case INDEPENDENT_DRIVER:
            case FLEET_DRIVER:

                deleteUserAssociatedData(user);
                userRepository.delete(user);
                break;


            case COMPANY_ADMIN:

                Company company = user.getCompany();

                if (company != null) {

                    UUID cid = company.getCompanyId();


                    int leaders =
                            userRepository.countByCompany_CompanyIdAndRoleInAndIsDeletedFalse(
                                    cid,
                                    List.of(Role.ADMIN, Role.COMPANY_ADMIN)
                            );


                    if (leaders <= 1) {

                        deleteCompanyData(company);

                    } else {

                        deleteUserAssociatedData(user);
                        userRepository.delete(user);
                    }

                } else {

                    deleteUserAssociatedData(user);
                    userRepository.delete(user);
                }

                break;


            case ADMIN:

                Company adminCompany = user.getCompany();


                if (adminCompany != null) {

                    UUID cid = adminCompany.getCompanyId();


                    long admins =
                            userRepository.countByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                                    cid,
                                    Role.ADMIN
                            );


                    // Last owner permanently gone
                    if (admins <= 1) {


                        Optional<User> replacement =
                                userRepository
                                        .findFirstByCompany_CompanyIdAndRoleAndIsDeletedFalseOrderByInsertedAtAsc(
                                                cid,
                                                Role.COMPANY_ADMIN
                                        );


                        if (replacement.isPresent()) {

                            // Promote company admin instead
                            User newOwner = replacement.get();

                            newOwner.setRole(Role.ADMIN);

                            userRepository.save(newOwner);


                            deleteUserAssociatedData(user);
                            userRepository.delete(user);


                        } else {

                            deleteCompanyData(adminCompany);
                        }

                    } else {

                        deleteUserAssociatedData(user);
                        userRepository.delete(user);
                    }


                } else {

                    deleteUserAssociatedData(user);
                    userRepository.delete(user);
                }


                break;
        }
    }


    private void deleteCompanyData(Company company) {

        UUID cid = company.getCompanyId();


        List<User> drivers =
                userRepository.findByCompany_CompanyIdAndRole(
                        cid,
                        Role.FLEET_DRIVER
                );


        for (User driver : drivers) {

            deleteUserAssociatedData(driver);
            userRepository.delete(driver);
        }


        List<User> admins =
                userRepository.findByCompany_CompanyIdAndRoleIn(
                        cid,
                        List.of(Role.ADMIN, Role.COMPANY_ADMIN)
                );


        for (User admin : admins) {

            deleteUserAssociatedData(admin);
            userRepository.delete(admin);
        }


        companyRepository.delete(company);
    }

    /**
     * Helper method to safely delete all child records before deleting the user.
     * Order matters to prevent Foreign Key constraint violations.
     */
    private void deleteUserAssociatedData(User user) {
        refreshTokenRepository.deleteByUser(user);
        detectionLogRepository.deleteByUser(user);
        sessionRepository.deleteByUserId(user.getUserId());
    }

    // ========================================================================
    // 3. RESTORE ACCOUNT (First-Come, First-Serve Admin Assignment)
    // ========================================================================
    @Transactional
    public void restoreAccount(String email, String password) {

        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Must actually be deleted
        if (!user.isDeleted()) {
            throw new IllegalStateException("Account is already active.");
        }

        // Verify credentials
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SecurityException("Invalid credentials.");
        }

        // Restore user
        user.setDeleted(false);
        user.setDeletedAt(null);

        // Company leader restoration
        if (user.getRole() == Role.ADMIN ||
                user.getRole() == Role.COMPANY_ADMIN) {

            Company company = user.getCompany();

            if (company != null) {

                // Restore company if it was soft deleted
                if (company.isDeleted()) {

                    company.setDeleted(false);
                    company.setDeletedAt(null);

                    companyRepository.save(company);

                    // Restore all soft-deleted fleet drivers
                    List<User> fleetDrivers =
                            userRepository.findByCompany_CompanyIdAndRoleIncludingDeleted(
                                    company.getCompanyId(),
                                    Role.FLEET_DRIVER
                            );

                    for (User driver : fleetDrivers) {

                        if (driver.isDeleted()) {
                            driver.setDeleted(false);
                            driver.setDeletedAt(null);

                            userRepository.save(driver);
                        }
                    }
                }

                // Role assignment logic

                if (user.getRole() == Role.ADMIN) {

                    // Original owner keeps ownership
                    user.setRole(Role.ADMIN);

                } else {

                    boolean activeAdminExists =
                            userRepository.existsByCompany_CompanyIdAndRoleAndIsDeletedFalse(
                                    company.getCompanyId(),
                                    Role.ADMIN
                            );

                    if (!activeAdminExists) {

                        // No active owner exists
                        // First company admin back becomes owner
                        user.setRole(Role.ADMIN);

                    } else {

                        // Owner already exists
                        user.setRole(Role.COMPANY_ADMIN);
                    }
                }
            }
        }

        userRepository.save(user);
    }

    // ========================================================================
    // GET COMPANY ADMINS
    // ========================================================================

    @Transactional
    public void addFleetDriver(FleetDriverDto req, String adminEmail) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // Map DTO to User
        User newDriver = new User();
        newDriver.setEmail(req.getEmail());
        newDriver.setFullName(req.getFullName());
        newDriver.setGender(req.getGender());
        newDriver.setRole(Role.FLEET_DRIVER);
        newDriver.setCompany(loggedInAdmin.getCompany());
        newDriver.setBirthDate(req.getBirthDate());

        String rawTempPassword = PasswordGeneratorUtil.generateCompliantPassword();
        newDriver.setPasswordHash(passwordEncoder.encode(rawTempPassword));

        newDriver.setStatus(UserStatus.ACTIVE);

        userRepository.save(newDriver);
        notificationService.sendWelcomePasswordSetEmail(newDriver.getEmail());
    }

    @Transactional
    public void deleteDriverByEmail(String adminEmail, String driverEmail) {
        // 1. Fetch the admin's company ID (Fixed: Changed Long to UUID)
        UUID adminCompanyId = userRepository.findCompanyIdByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        // 2. Fetch the full driver object
        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        // 3. Verify the user is actually a driver
        if (driver.getRole() != Role.FLEET_DRIVER) {
            throw new IllegalArgumentException("The specified user is not a driver.");
        }

        // 4. Compare company IDs (Fixed: Navigating through the Company object)
        if (driver.getCompany() == null || !adminCompanyId.equals(driver.getCompany().getCompanyId())) {
            throw new SecurityException("You are not authorized to delete a user from another company.");
        }

        // 5. Delete the account
        deleteAccount(driver.getUserId()); // Ensure your deleteAccount method accepts a UUID
    }

    public List<AdminListDto> getCompanyAdmins(String requesterEmail) {
        User requester = findByEmail(requesterEmail);

        if (requester.getCompany() == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        // Fetch users in the same company who are either ADMIN or COMPANY_ADMIN
        List<User> admins = userRepository.findByCompany_CompanyIdAndRoleIn(
                requester.getCompany().getCompanyId(),
                List.of(Role.ADMIN, Role.COMPANY_ADMIN)
        );

        return admins.stream()
                .map(user -> AdminListDto.builder()
                        .name(user.getFullName())
                        .email(user.getEmail())
                        // Note: If your User entity uses a different field name for the creation date
                        // (like getCreatedAt() or getJoinedAt()), update getInsertedAt() below to match.
                        .insertedAt(user.getInsertedAt())
                        .role(user.getRole().name())
                        .build())
                .toList();
    }

    // ========================================================================
    // GET COMPANY INFO
    // ========================================================================
    public CompanyInfoDto getCompanyInfo(String requesterEmail) {
        // 1. Get the logged-in user and their company
        User requester = findByEmail(requesterEmail);
        Company company = requester.getCompany();

        if (company == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        UUID companyId = company.getCompanyId();

        // 2. Count the total number of admins (ADMIN + COMPANY_ADMIN)
        int totalAdmins = userRepository.countByCompany_CompanyIdAndRoleIn(
                companyId,
                List.of(Role.ADMIN, Role.COMPANY_ADMIN)
        );

        // 3. Find the single primary ADMIN for this company
        User primaryAdmin = userRepository.findByCompany_CompanyIdAndRole(companyId, Role.ADMIN)
                .stream()
                .findFirst() // Grabs the first item from the List (returns an Optional)
                .orElseThrow(() -> new IllegalStateException("Company has no primary ADMIN (Owner)."));

        // 4. Map everything to the DTO
        return CompanyInfoDto.builder()
                .companyName(company.getName())
//                .companyAddress(company.getAddress())
                .totalAdmins(totalAdmins)
                // Note: If your Company entity uses getCreatedAt(), change this to match
                .companyInsertedAt(company.getInsertedAt())

                .adminName(primaryAdmin.getFullName())
                .adminEmail(primaryAdmin.getEmail())
                .build();
    }

    public AdminCompanyDashboardDto getAdminCompanyDashboard(String email) {

        User requester = findByEmail(email);

        if (requester.getRole() != Role.ADMIN &&
                requester.getRole() != Role.COMPANY_ADMIN) {

            throw new IllegalStateException(
                    "User is not allowed to access this page"
            );
        }


        CompanyInfoDto company =
                getCompanyInfo(email);

        List<AdminListDto> admins =
                getCompanyAdmins(email);

        UserProfileResponseDto user =
                getUserProfileDto(email);


        return AdminCompanyDashboardDto.builder()
                .company(company)
                .admins(admins)
                .user(user)
                .build();
    }
}
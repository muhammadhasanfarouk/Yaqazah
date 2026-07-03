package com.yaqazah.user.service;

import com.yaqazah.common.util.PasswordGeneratorUtil;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.request.CompanyAdminDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@NullMarked
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final CompanyService companyService;
    private final UserService userService;

//    @Transactional
//    @Caching(evict = {
//            @CacheEvict(value = "dashboard",              allEntries = true),
//            @CacheEvict(value = "admin:sessions",         allEntries = true),
//            @CacheEvict(value = "admin:session-detail",   allEntries = true),
//            @CacheEvict(value = "admin:drivers",          allEntries = true),
//            @CacheEvict(value = "admin:driver-detail",    allEntries = true),
//            @CacheEvict(value = "user:analytics",         allEntries = true),
//            @CacheEvict(value = "user:sessions",          allEntries = true),
//            @CacheEvict(value = "user:session-detail",    allEntries = true)
//    })
//    public void registerCompanyOwner(CompanyOwnerRegistrationDto req) {
//        // 1. Validate email first
//        if (userRepository.findByEmail(req.getAdminEmail()).isPresent()) {
//            throw new IllegalArgumentException("Email is already taken!");
//        }
//
//        // 2. Create the Company
//        Company newCompany = new Company();
//        newCompany.setName(req.getCompanyName());

    /// /        newCompany.setAddress(req.getCompanyAddress());
//
//        Company savedCompany = companyService.createCompany(newCompany);
//
//        // 3. Map DTO to User
//        User newAdmin = new User();
//        newAdmin.setEmail(req.getAdminEmail());
//        newAdmin.setFullName(req.getAdminFullName());
//        newAdmin.setGender(req.getAdminGender());
//        newAdmin.setRole(Role.COMPANY_ADMIN);
//        newAdmin.setCompany(savedCompany);
//        newAdmin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
//        newAdmin.setStatus(UserStatus.ACTIVE);
//        newAdmin.setBirthDate(req.getBirthDate());
//
//        // 4. Save to DB
//        userRepository.save(newAdmin);
//    }
    @Transactional
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
    public void addCompanyAdmin(CompanyAdminDto req, String adminEmail) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // Map DTO to User
        User newCompanyAdmin = new User();
        newCompanyAdmin.setEmail(req.getEmail());
        newCompanyAdmin.setFullName(req.getFullName());
        newCompanyAdmin.setGender(req.getGender());
        newCompanyAdmin.setRole(Role.COMPANY_ADMIN);
        newCompanyAdmin.setCompany(loggedInAdmin.getCompany()); // Link to same company
        newCompanyAdmin.setBirthDate(req.getBirthDate());

        // Auto-generate password
        String rawTempPassword = PasswordGeneratorUtil.generateCompliantPassword();
        newCompanyAdmin.setPasswordHash(passwordEncoder.encode(rawTempPassword));

        newCompanyAdmin.setStatus(UserStatus.ACTIVE);

        userRepository.save(newCompanyAdmin);
        notificationService.sendWelcomePasswordSetEmail(newCompanyAdmin.getEmail());
    }

//    @Transactional
//    public void addFleetDriver(FleetDriverDto req, String adminEmail) {
//        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
//            throw new IllegalArgumentException("Email is already taken!");
//        }
//
//        User loggedInAdmin = userRepository.findByEmail(adminEmail)
//                .orElseThrow(() -> new IllegalStateException("Admin user not found."));
//
//        // Map DTO to User
//        User newDriver = new User();
//        newDriver.setEmail(req.getEmail());
//        newDriver.setFullName(req.getFullName());
//        newDriver.setGender(req.getGender());
//        newDriver.setRole(Role.FLEET_DRIVER);
//        newDriver.setCompany(loggedInAdmin.getCompany());
//        newDriver.setBirthDate(req.getBirthDate());
//
//        String rawTempPassword = PasswordGeneratorUtil.generateCompliantPassword();
//        newDriver.setPasswordHash(passwordEncoder.encode(rawTempPassword));
//
//        newDriver.setStatus(UserStatus.ACTIVE);
//
//        userRepository.save(newDriver);
//        notificationService.sendWelcomePasswordSetEmail(newDriver.getEmail());
//    }

    @Transactional
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
    public void swapOwnership(String currentAdminEmail, String targetEmail) {

        User currentAdmin = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Current admin not found."));

        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

        // Validations
        if (currentAdmin.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the current ADMIN can transfer ownership.");
        }
        if (targetUser.getRole() != Role.COMPANY_ADMIN) {
            throw new IllegalStateException("Ownership can only be transferred to a COMPANY_ADMIN.");
        }
        if (currentAdmin.getCompany() == null || targetUser.getCompany() == null ||
                !currentAdmin.getCompany().getCompanyId().equals(targetUser.getCompany().getCompanyId())) {
            throw new IllegalStateException("Both users must belong to the same company to swap roles.");
        }

        // Perform the Swap
        currentAdmin.setRole(Role.COMPANY_ADMIN);
        targetUser.setRole(Role.ADMIN);

        // Save the changes
        userRepository.save(currentAdmin);
        userRepository.save(targetUser);
    }

    // Add this inside com.yaqazah.user.service.UserService

    @Transactional
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
    public void deleteCompanyAdminByEmail(String requesterEmail, String targetAdminEmail) {
        // 1. Fetch the requester's company ID (Fixed: Changed Long to UUID)
        UUID requesterCompanyId = userRepository.findCompanyIdByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));

        // 2. Fetch the full target admin object
        User targetAdmin = userRepository.findByEmail(targetAdminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Target admin not found"));

        // 3. Verify the target user is actually a company admin
        if (targetAdmin.getRole() != Role.COMPANY_ADMIN) {
            throw new IllegalArgumentException("The specified user is not a company admin.");
        }

        // 4. Compare company IDs (Fixed: Navigating through the Company object)
        if (targetAdmin.getCompany() == null || !requesterCompanyId.equals(targetAdmin.getCompany().getCompanyId())) {
            throw new SecurityException("You are not authorized to delete an admin from another company.");
        }

        // 5. Delete the account
        userService.deleteAccount(targetAdmin.getUserId()); // Ensure your deleteAccount method accepts a UUID
    }
}
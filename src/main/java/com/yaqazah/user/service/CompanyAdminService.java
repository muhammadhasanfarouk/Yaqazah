package com.yaqazah.user.service;

import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.request.UpdateFleetDriverDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
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
@RequiredArgsConstructor
@NullMarked
public class CompanyAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

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
    public void updateFleetDriver(UUID driverId, UpdateFleetDriverDto updatedData, String adminEmail) {
        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        User existingDriver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

        // Security Check 1: Is this a fleet driver?
        if (existingDriver.getRole() != Role.FLEET_DRIVER) {
            throw new IllegalArgumentException("Target user is not a fleet driver.");
        }

        // Security Check 2: Does this driver belong to the Admin's company?
        if (existingDriver.getCompany() == null ||
                !existingDriver.getCompany().getCompanyId().equals(loggedInAdmin.getCompany().getCompanyId())) {
            throw new IllegalStateException("You are not authorized to edit drivers outside your company.");
        }

        // Apply partial updates from DTO
        if (updatedData.getFullName() != null && !updatedData.getFullName().isBlank()) {
            existingDriver.setFullName(updatedData.getFullName());
        }
        if (updatedData.getGender() != null) {
            existingDriver.setGender(updatedData.getGender());
        }
        if (updatedData.getStatus() != null) {
            existingDriver.setStatus(updatedData.getStatus());
        }

        // Only update password if one was explicitly provided
        if (updatedData.getNewPassword() != null && !updatedData.getNewPassword().trim().isEmpty()) {
            existingDriver.setPasswordHash(passwordEncoder.encode(updatedData.getNewPassword()));
        }

        userRepository.save(existingDriver);
    }
}
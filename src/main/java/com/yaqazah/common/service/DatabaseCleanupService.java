package com.yaqazah.common.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import com.yaqazah.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseCleanupService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserService userService;

    /**
     * Removes users that never verified their account after 24 hours.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupPendingVerificationUsers() {

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

        List<User> users = userRepository.findByStatusAndInsertedAtBefore(
                UserStatus.PENDING_VERIFICATION,
                cutoff
        );

        for (User user : users) {

            Company company = user.getCompany();

            userRepository.delete(user);

            if (company != null && !userRepository.existsByCompany(company)) {
                companyRepository.delete(company);
            }
        }
    }

    /**
     * Permanently deletes soft-deleted accounts after 30 days.
     * Runs every day at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
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
    public void cleanupSoftDeletedUsers() {

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        List<User> users = userRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        for (User user : users) {
            userService.hardDeleteAccount(user.getUserId());
        }
    }
}
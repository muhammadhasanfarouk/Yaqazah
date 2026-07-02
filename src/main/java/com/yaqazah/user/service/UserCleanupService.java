package com.yaqazah.user.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanup() {

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
}
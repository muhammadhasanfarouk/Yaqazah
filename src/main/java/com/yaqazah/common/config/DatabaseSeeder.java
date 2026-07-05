package com.yaqazah.common.config;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.dashboard.util.AlertTypeMapper;
import com.yaqazah.session.model.Session;
import com.yaqazah.session.repository.SessionRepository;
import com.yaqazah.user.model.Gender;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    // use this as password for admins: $2a$10$ockpBu23D856jpBmJkwg8eyWgDuOZPaQcgAAoLbA445XDMcerEDna
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SessionRepository sessionRepository;
    private final DetectionLogRepository detectionLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@yaqazah.com";
        if (userRepository.findByEmailIncludingDeleted(adminEmail).isPresent()) {
            log.info("Admin user already exists. Continuing with data seeding.");
        }

        log.info("Starting database seeding for 40 drivers...");
        seedData();
        log.info("Database seeding completed successfully!");
    }

    @Transactional
    public void seedData() {
        // 1. Create Companies
        Company yaqazahFleet = getOrCreateCompany("Yaqazah");

        // Use the requested password for all seeded users
        String defaultEncodedPassword = passwordEncoder.encode("MuhammadHasan123");
        String adminEncodedPassword = passwordEncoder.encode("password123");

        // 2. Create Admin Users
        createUser("admin@yaqazah.com", "Global Admin", Role.ADMIN, Gender.MALE, null, adminEncodedPassword);
        createUser("fleetadmin@yaqazah.com", "Yaqazah Fleet Admin", Role.COMPANY_ADMIN, Gender.MALE, yaqazahFleet, adminEncodedPassword);

        // 3. Define Driver Data
        List<DriverConfig> fleetDrivers = Arrays.asList(
                new DriverConfig("Ahmed Ali", "ahmed.ali@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Omar Hassan", "omar.hassan@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Youssef Mahmoud", "youssef.mahmoud@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Mariam Ahmed", "mariam.ahmed@yaqazah.com", DriverProfile.SAFE, Gender.FEMALE),
                new DriverConfig("Fatma Khaled", "fatma.khaled@yaqazah.com", DriverProfile.SAFE, Gender.FEMALE),
                new DriverConfig("Ibrahim Salem", "ibrahim.salem@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Nour ElSayed", "nour.elsayed@yaqazah.com", DriverProfile.SAFE, Gender.FEMALE),
                new DriverConfig("Mahmoud Adel", "mahmoud.adel@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Karim Mostafa", "karim.mostafa@yaqazah.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Sara Mohamed", "sara.mohamed@yaqazah.com", DriverProfile.SAFE, Gender.FEMALE),
                new DriverConfig("Mohamed Tarek", "mohamed.tarek@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Mostafa Nabil", "mostafa.nabil@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Ali Gamal", "ali.gamal@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Hassan Fathy", "hassan.fathy@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Amr Samir", "amr.samir@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Salma Wael", "salma.wael@yaqazah.com", DriverProfile.MODERATE, Gender.FEMALE),
                new DriverConfig("Menna Ashraf", "menna.ashraf@yaqazah.com", DriverProfile.MODERATE, Gender.FEMALE),
                new DriverConfig("Dina Osama", "dina.osama@yaqazah.com", DriverProfile.MODERATE, Gender.FEMALE),
                new DriverConfig("Hany Saber", "hany.saber@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Tamer Atef", "tamer.atef@yaqazah.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Khaled Saeed", "khaled.saeed@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Ahmed Samy", "ahmed.samy@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Wael Ibrahim", "wael.ibrahim@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Sherif Adel", "sherif.adel@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Mina Nader", "mina.nader@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("George Fawzy", "george.fawzy@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Ramy Emad", "ramy.emad@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Basem Youssef", "basem.youssef@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Hossam Farouk", "hossam.farouk@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Adel Reda", "adel.reda@yaqazah.com", DriverProfile.UNSAFE, Gender.MALE)
        );

        List<DriverConfig> independentDrivers = Arrays.asList(
                new DriverConfig("Tarek Hussein", "tarek.hussein@gmail.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Aya Mahmoud", "aya.mahmoud@gmail.com", DriverProfile.SAFE, Gender.FEMALE),
                new DriverConfig("Mahmoud Ehab", "mahmoud.ehab@gmail.com", DriverProfile.SAFE, Gender.MALE),
                new DriverConfig("Rana Adel", "rana.adel@gmail.com", DriverProfile.MODERATE, Gender.FEMALE),
                new DriverConfig("Hoda Salah", "hoda.salah@gmail.com", DriverProfile.MODERATE, Gender.FEMALE),
                new DriverConfig("Islam Mostafa", "islam.mostafa@gmail.com", DriverProfile.MODERATE, Gender.MALE),
                new DriverConfig("Ahmed Magdy", "ahmed.magdy@gmail.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Eslam Nasser", "eslam.nasser@gmail.com", DriverProfile.UNSAFE, Gender.MALE),
                new DriverConfig("Nada Fathy", "nada.fathy@gmail.com", DriverProfile.UNSAFE, Gender.FEMALE),
                new DriverConfig("Mahmoud Ashour", "mahmoud.ashour@gmail.com", DriverProfile.UNSAFE, Gender.MALE)
        );

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 30);

        // 4. Seed Fleet Drivers (Assigned to Yaqazah Company)
        for (DriverConfig config : fleetDrivers) {
            User driver = createUser(config.email(), config.name(), Role.FLEET_DRIVER, config.gender(), yaqazahFleet, defaultEncodedPassword);
            seedDriverSessions(driver, startDate, endDate, config.profile());
        }

        // 5. Seed Independent Drivers (No Company)
        for (DriverConfig config : independentDrivers) {
            User driver = createUser(config.email(), config.name(), Role.INDEPENDENT_DRIVER, config.gender(), null, defaultEncodedPassword);
            seedDriverSessions(driver, startDate, endDate, config.profile());
        }
    }

    private User createUser(String email, String fullName, Role role, Gender gender, Company company, String passwordHash) {
        return userRepository.findByEmailIncludingDeleted(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setFullName(fullName);
                    user.setPasswordHash(passwordHash);
                    user.setRole(role);
                    user.setGender(gender);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setCompany(company);
                    user.setBirthDate(LocalDate.of(1985 + new Random().nextInt(15), 1 + new Random().nextInt(11), 1 + new Random().nextInt(27)));
                    user.setDeleted(false);
                    return userRepository.save(user);
                });
    }

    private Company getOrCreateCompany(String name) {
        return companyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Company company = new Company();
                    company.setName(name);
                    return companyRepository.save(company);
                });
    }

    private enum DriverProfile {
        SAFE, MODERATE, UNSAFE
    }

    private record DriverConfig(String name, String email, DriverProfile profile, Gender gender) {}

    private void seedDriverSessions(User driver, LocalDate start, LocalDate end, DriverProfile profile) {
        List<Session> existingSessions = sessionRepository.findByUserId(driver.getUserId());
        if (!existingSessions.isEmpty()) {
            log.info("Driver {} already has sessions. Skipping session seeding for this driver.", driver.getEmail());
            return;
        }

        Random random = new Random(driver.getEmail().hashCode()); // Ensure deterministic seeding for identical email
        List<Session> sessions = new ArrayList<>();
        List<DetectionLog> logs = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            // Decide if the driver drives on this day (probability depending on profile/weekend)
            double probability = 0.25;
            if (profile == DriverProfile.SAFE) {
                probability = 0.35; // Safe drivers drive more often
            } else if (profile == DriverProfile.UNSAFE) {
                probability = 0.15; // Unsafe drivers drive less often or are suspended
            }

            if (random.nextDouble() > probability) {
                continue;
            }

            // Create a session
            Session session = new Session();
            session.setUserId(driver.getUserId());

            int startHour = 7 + random.nextInt(12); // starts between 7:00 AM and 7:00 PM
            int startMinute = random.nextInt(60);
            Instant sessionStart = date.atTime(startHour, startMinute).atZone(ZoneOffset.UTC).toInstant();
            session.setStartDateTime(sessionStart.toString());

            double durationHours = 0.5 + random.nextDouble() * 6.5; // 30 mins to 7 hours
            session.setDurationHours(durationHours);

            long durationSeconds = (long) (durationHours * 3600.0);
            Instant sessionEnd = sessionStart.plusSeconds(durationSeconds);
            session.setEndDateTime(sessionEnd.toString());

            session.setInsertionTimestamp(Instant.now().toString());

            // Determine alert events during the trip
            int alertCount = 0;
            if (profile == DriverProfile.SAFE) {
                // very low chance of alerts, mostly low risk (0-1 alerts)
                if (random.nextDouble() < 0.1) {
                    alertCount = 1;
                }
            } else if (profile == DriverProfile.MODERATE) {
                // moderate alert count (0 to 3 alerts)
                alertCount = random.nextInt(4);
            } else {
                // unsafe alert count (2 to 8 alerts)
                alertCount = 2 + random.nextInt(7);
            }

            session.setTotalAlerts(alertCount);
            Session savedSession = sessionRepository.save(session);
            sessions.add(savedSession);

            for (int i = 0; i < alertCount; i++) {
                DetectionLog log = new DetectionLog();
                log.setSession(savedSession);
                log.setUser(driver);
                log.setInsertionTimestamp(Instant.now().toString());

                // Random timestamp during the session
                long offsetSeconds = (long) (random.nextDouble() * durationSeconds);
                log.setTimestamp(sessionStart.plusSeconds(offsetSeconds).toString());

                // Choose alert type based on profile
                DetectionType type = chooseDetectionType(random, profile);
                Integer alertId = AlertTypeMapper.toTypeId(type);
                if (alertId == null) {
                    // fallback if AlertTypeMapper returns null
                    alertId = 2; // Distraction
                    type = DetectionType.DISTRACTION;
                }

                log.setAlertId(alertId);
                log.setRiskId(getRiskId(type));
                log.setTitle(getAlertTitle(type));
                log.setSubtitle(getAlertSubtitle(type, random));
                log.setSnapshotUrl(getAlertSnapshot(type));

                logs.add(log);
            }
        }

        if (!logs.isEmpty()) {
            detectionLogRepository.saveAll(logs);
        }
    }

    private DetectionType chooseDetectionType(Random random, DriverProfile profile) {
        if (profile == DriverProfile.SAFE) {
            // safe drivers mostly do phone/looking_away/eating
            double val = random.nextDouble();
            if (val < 0.4) return DetectionType.EATING_AND_DRINKING;
            if (val < 0.8) return DetectionType.LOOKING_AWAY;
            return DetectionType.PHONE;
        } else if (profile == DriverProfile.MODERATE) {
            double val = random.nextDouble();
            if (val < 0.3) return DetectionType.DROWSINESS;
            if (val < 0.6) return DetectionType.LOOKING_AWAY;
            if (val < 0.8) return DetectionType.PHONE;
            return DetectionType.DISTRACTION;
        } else {
            // unsafe drivers have sleepiness and drowsiness and distraction
            double val = random.nextDouble();
            if (val < 0.3) return DetectionType.SLEEPINESS;
            if (val < 0.6) return DetectionType.DROWSINESS;
            if (val < 0.8) return DetectionType.DISTRACTION;
            return DetectionType.PHONE;
        }
    }

    private int getRiskId(DetectionType type) {
        return switch (type) {
            case SLEEPINESS -> 3; // Critical
            case DROWSINESS -> 1; // Medium
            case DISTRACTION -> 2; // High
            case PHONE -> 1;      // Medium
            case LOOKING_AWAY -> 0; // Low
            case EATING_AND_DRINKING -> 0; // Low
            default -> 0;
        };
    }

    private String getAlertTitle(DetectionType type) {
        return switch (type) {
            case SLEEPINESS -> "Sleepiness Detected";
            case DROWSINESS -> "Drowsiness Warning";
            case DISTRACTION -> "Distraction Alert";
            case PHONE -> "Mobile Phone Usage";
            case LOOKING_AWAY -> "Looking Away";
            case EATING_AND_DRINKING -> "Eating / Drinking";
            default -> "Safety Alert";
        };
    }

    private String getAlertSubtitle(DetectionType type, Random random) {
        double duration = 1.0 + random.nextDouble() * 4.0;
        String durStr = String.format(Locale.US, "%.1f", duration);
        return switch (type) {
            case SLEEPINESS -> "Driver closed eyes for " + durStr + " seconds";
            case DROWSINESS -> "Yawning and head dropping detected";
            case DISTRACTION -> "Driver diverted attention for " + durStr + " seconds";
            case PHONE -> "Phone usage identified while vehicle in motion";
            case LOOKING_AWAY -> "Driver looked away from road for " + durStr + " seconds";
            case EATING_AND_DRINKING -> "Eating/drinking while driving";
            default -> "Unsafe driving event detected";
        };
    }

    private String getAlertSnapshot(DetectionType type) {
        return switch (type) {
            case SLEEPINESS, DROWSINESS -> "https://yaqazahstorage.blob.core.windows.net/snapshots/drowsiness_event.jpg";
            case DISTRACTION, LOOKING_AWAY -> "https://yaqazahstorage.blob.core.windows.net/snapshots/distraction_event.jpg";
            case PHONE -> "https://yaqazahstorage.blob.core.windows.net/snapshots/phone_event.jpg";
            case EATING_AND_DRINKING -> "https://yaqazahstorage.blob.core.windows.net/snapshots/eating_event.jpg";
            default -> "https://yaqazahstorage.blob.core.windows.net/snapshots/default_event.jpg";
        };
    }
}
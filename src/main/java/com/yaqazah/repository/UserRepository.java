package com.yaqazah.repository;

import com.yaqazah.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Find by email for login and reset password [cite: 128, 129, 336]
    Optional<User> findByEmail(String email);

    // Search for drivers by username for the Admin Web Portal [cite: 139]
    Optional<User> findByUsername(String username);
}
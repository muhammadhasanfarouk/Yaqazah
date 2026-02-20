package com.yaqazah.user.repository;

import com.yaqazah.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Find by email for login and reset password
    Optional<User> findByEmail(String email);

    // Search for drivers by username for the Admin Web Portal
    Optional<User> findByUsername(String username);
}
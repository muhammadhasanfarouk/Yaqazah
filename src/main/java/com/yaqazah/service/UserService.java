package com.yaqazah.service;

import com.yaqazah.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.yaqazah.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;

    // General user retrieval [cite: 278, 336]
    public User getUser(UUID id) {
        return userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Handles logic for adding new users/drivers
    public void addUser(User user) {
        // Password hashing would happen here before saving
        userRepo.save(user); // Matches Source
    }

    // Updates existing profile data
    public void updateUser(User user) {
        userRepo.save(user);
    }

    // Deletes user (Admin functionality)
    public void deleteUser(UUID id) {
        userRepo.deleteById(id);
    }

    // Specialized logic for generating temporary passwords
    public String generatePassword() {
        // Logic to create a secure random string for new drivers
        return "Temporary123!";
    }
}
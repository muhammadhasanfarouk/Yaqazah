package com.yaqazah.controller;

import com.yaqazah.model.Driver;
import com.yaqazah.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.yaqazah.model.User;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService; // Matches Source 9

    @GetMapping("/hello")
    public ResponseEntity<String> hello()
    {
        return ResponseEntity.ok("Hello from UserController!");
    }
    // 1. General Profile Management for all Users
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId)); // Matches Source 9
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateUser(@RequestBody User user) {
        userService.updateUser(user); // Matches Source 9
        return ResponseEntity.noContent().build();
    }

    // 2. Specialized Registration (Admin adding Drivers)
    @PostMapping("/add-driver")
    @PreAuthorize("hasRole('COMPANYADM')") // Security layer for Web Admin
    public ResponseEntity<Void> addDriver(@RequestBody Driver driver) {
        userService.addUser(driver); // Matches Source 9
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3. Admin Functionality: Search and Manage Drivers
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('COMPANYADM')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId); // Matches Source 9
        return ResponseEntity.noContent().build();
    }
}
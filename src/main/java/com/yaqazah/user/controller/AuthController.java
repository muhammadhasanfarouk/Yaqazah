package com.yaqazah.user.controller;

import com.yaqazah.user.model.Driver;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register-driver")
    public void registerDriver(@RequestBody Driver driverData) {
        // Implementation [cite: 264]
    }

    @PostMapping("/login")
    public void login(@RequestParam String email, @RequestParam String password) {
        // Implementation [cite: 265]
    }

    @PostMapping("/logout")
    public void logout() {
        // Implementation [cite: 265]
    }
}
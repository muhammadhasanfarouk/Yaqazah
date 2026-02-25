package com.yaqazah.user.controller;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. SIGN UP [cite: 161, 264]
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already taken!");
        }

        // Hash the password before saving to the database
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        userRepository.save(user); // Save to DB
        return ResponseEntity.ok("User registered successfully!");
    }

    // 2. LOG IN [cite: 167, 265]
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String password) {
        // Authenticate the user (Spring checks the hashed password automatically)
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        // If successful, generate the JWT
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final String jwt = jwtUtil.generateToken(userDetails);

        // Return the token to the Flutter app/Web frontend
        return ResponseEntity.ok(jwt);
    }
}
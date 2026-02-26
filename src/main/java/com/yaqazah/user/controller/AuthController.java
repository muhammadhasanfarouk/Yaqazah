package com.yaqazah.user.controller;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
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

    // 1. SIGN UP
    @Operation(summary = "Register a new user", description = "Creates a new user in the database. Will fail if the email is already in use.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully!"),
            @ApiResponse(responseCode = "400", description = "Email is already taken!")
    })
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

    // 2. LOG IN
    @Operation(summary = "Log in to get JWT Token", description = "Send email and password as form parameters to receive a Bearer token for accessing protected routes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned the JWT Token"),
            @ApiResponse(responseCode = "403", description = "Bad credentials (wrong email or password)")
    })
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
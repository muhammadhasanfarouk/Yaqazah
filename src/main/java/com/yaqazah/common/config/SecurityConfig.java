package com.yaqazah.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Disable for development/APIs
                .authorizeHttpRequests(auth -> auth
                        // Allow anyone to see your "hello" test
                        .requestMatchers("/api/users/hello").permitAll()

                        // Registration and Login should be public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Admin specific views (Web)
                        .requestMatchers("/api/admin/**").hasRole("COMPANYADM")

                        // Driver specific views (Mobile)
                        .requestMatchers("/api/sessions/**").hasRole("DRIVER")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // Allows testing via Postman/Browser

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Essential for storing 'password_hash' in your DB
        return new BCryptPasswordEncoder();
    }
}
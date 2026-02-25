package com.yaqazah.common.config;

import com.yaqazah.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                // 1. Public Endpoints (No token needed)
                .requestMatchers("/api/auth/**").permitAll()

                // 2. Admin-Only Endpoints (Must have JWT token AND be a COMPANYADM)
                .requestMatchers("/api/analytics/company/**").hasRole("COMPANYADM")
                .requestMatchers("/api/reports/**").hasRole("COMPANYADM")

                // 3. Driver-Only Endpoints (Must have JWT token AND be a DRIVER)
                .requestMatchers("/api/sessions/**").hasRole("DRIVER")

                // 4. Any other endpoint just requires a valid JWT token
                .anyRequest().authenticated()
        );

        // Add the JWT filter BEFORE the standard username/password filter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expose AuthenticationManager to be used in your login controller
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
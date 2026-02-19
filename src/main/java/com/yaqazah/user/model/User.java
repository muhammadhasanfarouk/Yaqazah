package com.yaqazah.user.model;

import jakarta.persistence.*;

import java.util.UUID;

// Base User Entity
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userId;

    private String email;
    private String passwordHash;
    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;
}
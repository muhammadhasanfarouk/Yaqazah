package com.yaqazah.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.UUID;

// Driver Subclass
@Entity
@Table(name = "drivers")
@PrimaryKeyJoinColumn(name = "user_id") // Links to User table
public class Driver extends User {
    private boolean isFleetDriver;
    private UUID companyId;

    // One Driver can have many driving sessions
//    @OneToMany(mappedBy = "driver")
//    private List<Session> sessions;
}

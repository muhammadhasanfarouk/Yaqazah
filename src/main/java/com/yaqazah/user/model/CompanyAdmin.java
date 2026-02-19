package com.yaqazah.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.UUID;

// Company Admin Subclass
@Entity
@Table(name = "company_admins")
@PrimaryKeyJoinColumn(name = "user_id")
public class CompanyAdmin extends User {
    private UUID companyId;
}

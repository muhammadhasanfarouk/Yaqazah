package com.yaqazah.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyInfoDto {
    // --- Company Details ---
    private String companyName;
    //    private String companyAddress;
    private int totalAdmins;
    private Instant companyInsertedAt;

    // --- Primary Admin (Owner) Details ---
    private String adminName;
    private String adminEmail;
}
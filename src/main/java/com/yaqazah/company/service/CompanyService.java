package com.yaqazah.company.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@NullMarked
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository; // Added this to verify user company

    @Transactional
    public Company createCompany(Company company) {
        if (companyRepository.existsByNameIgnoreCase(company.getName())) {
            throw new IllegalArgumentException("A company with this name already exists.");
        }

        if (company.getInsertedAt() == null) {
            company.setInsertedAt(Instant.now());
        }

        return companyRepository.save(company);
    }

//    public Optional<Company> getCompanyById(UUID companyId, String userEmail) {
//        User loggedInUser = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new IllegalStateException("User not found."));
//
//        // BOTH Admin and Company Admin must pass this check
//        if (loggedInUser.getCompany() == null || !loggedInUser.getCompany().getCompanyId().equals(companyId)) {
//            throw new IllegalStateException("Access Denied: You are not assigned to this company.");
//        }
//
//        return companyRepository.findById(companyId);
//    }

//    @Transactional
//    public void deleteCompany(UUID companyId, String userEmail) {
//        User loggedInUser = userRepository.findByEmail(userEmail)
//                .orElseThrow(() -> new IllegalStateException("User not found."));
//
//        //Admin must pass this check
//        if (loggedInUser.getCompany() == null || !loggedInUser.getCompany().getCompanyId().equals(companyId)) {
//            throw new IllegalStateException("Access Denied: You can only delete your own company.");
//        }
//
//        if (!companyRepository.existsById(companyId)) {
//            throw new IllegalArgumentException("Company not found.");
//        }
//        companyRepository.deleteById(companyId);
//    }
}
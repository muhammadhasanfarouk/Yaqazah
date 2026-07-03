package com.yaqazah.company.repository;

import com.yaqazah.company.model.Company;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@NullMarked
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    // Spring Data JPA automatically provides save(), findById(), and findAll().
    // You only need to declare custom queries here.

    // Example: If you wanted to check if a company name already exists
    boolean existsByNameIgnoreCase(String name);

    // Example: If you wanted to find a company by its name
    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByCompanyId(UUID companyId);
}
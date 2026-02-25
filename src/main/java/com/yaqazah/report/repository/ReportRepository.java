package com.yaqazah.report.repository;

import com.yaqazah.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    // Custom query methods if needed
}
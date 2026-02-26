package com.yaqazah.report.controller;

import com.yaqazah.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Endpoints for generating system reports in various formats. Restricted to Company Admins.")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Operation(summary = "Generate PDF Report", description = "Triggers the generation of a system report in PDF format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF report generated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires COMPANYADM role)")
    })
    @GetMapping("/pdf")
    public void generatePDFReport() {
        reportService.generatePDFReport();
    }

    @Operation(summary = "Generate CSV Report", description = "Triggers the generation of a system report in CSV format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV report generated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires COMPANYADM role)")
    })
    @GetMapping("/csv")
    public void generateCSVReport() {
        reportService.generateCSVReport();
    }
}
package com.yaqazah.report.controller;

import com.yaqazah.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/pdf")
    public void generatePDFReport() { reportService.generatePDFReport(); }

    @GetMapping("/csv")
    public void generateCSVReport() { reportService.generateCSVReport(); }
}
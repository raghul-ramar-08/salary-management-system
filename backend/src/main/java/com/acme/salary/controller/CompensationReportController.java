package com.acme.salary.controller;

import com.acme.salary.dto.CountrySalaryReportResponse;
import com.acme.salary.dto.DepartmentSalaryReportResponse;
import com.acme.salary.service.CompensationReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class CompensationReportController {
    private final CompensationReportService reportService;

    public CompensationReportController(CompensationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping({"/countries", "/salary/by-country"})
    public CountrySalaryReportResponse countryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return reportService.countryReport(asOfDate, includeInactive);
    }

    @GetMapping({"/departments", "/salary/by-department"})
    public DepartmentSalaryReportResponse departmentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return reportService.departmentReport(asOfDate, includeInactive);
    }
}

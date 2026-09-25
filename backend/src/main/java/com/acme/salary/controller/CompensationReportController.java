package com.acme.salary.controller;

import com.acme.salary.dto.CountrySalaryReportResponse;
import com.acme.salary.dto.DepartmentSalaryExtremesReportResponse;
import com.acme.salary.dto.DepartmentSalaryReportResponse;
import com.acme.salary.dto.SalaryDistributionReportResponse;
import com.acme.salary.service.CompensationReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reports")
public class CompensationReportController {
    private static final Logger log = LoggerFactory.getLogger(CompensationReportController.class);
    private final CompensationReportService reportService;

    public CompensationReportController(CompensationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping({"/countries", "/salary/by-country"})
    public CountrySalaryReportResponse countryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.debug("Country salary report requested: asOfDate={}, includeInactive={}", asOfDate, includeInactive);
        return reportService.countryReport(asOfDate, includeInactive);
    }

    @GetMapping({"/departments", "/salary/by-department"})
    public DepartmentSalaryReportResponse departmentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.debug("Department salary report requested: asOfDate={}, includeInactive={}", asOfDate, includeInactive);
        return reportService.departmentReport(asOfDate, includeInactive);
    }

    @GetMapping({"/distribution", "/salary/distribution"})
    public SalaryDistributionReportResponse distributionReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) BigDecimal bandSize,
            @RequestParam(required = false) BigDecimal bucketSize,
            @RequestParam(required = false) List<BigDecimal> bands) {
        BigDecimal effectiveBandSize = bandSize != null ? bandSize : bucketSize;
        log.debug("Salary distribution report requested: asOfDate={}, includeInactive={}, countryCode={}, bandSize={}, customBands={}",
                asOfDate, includeInactive, countryCode, effectiveBandSize, bands != null ? bands.size() : 0);
        return reportService.distributionReport(asOfDate, includeInactive, countryCode, effectiveBandSize, bands);
    }

    @GetMapping({"/department-extremes", "/salary/outliers"})
    public DepartmentSalaryExtremesReportResponse departmentExtremesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "3") int limit) {
        log.debug("Department extremes report requested: asOfDate={}, includeInactive={}, department={}, limit={}",
                asOfDate, includeInactive, department, limit);
        return reportService.departmentExtremesReport(asOfDate, includeInactive, department, limit);
    }
}

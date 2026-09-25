package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DepartmentSalaryReportResponse(
        LocalDate asOfDate,
        boolean includeInactive,
        String reportingCurrency,
        String rateBasis,
        Map<String, BigDecimal> exchangeRatesToReportingCurrency,
        long totalHeadcount,
        BigDecimal totalPayrollReportingCurrency,
        List<DepartmentSalaryMetrics> departments) {

    public record DepartmentSalaryMetrics(
            String department,
            long headcount,
            BigDecimal averageSalaryReportingCurrency,
            BigDecimal medianSalaryReportingCurrency,
            BigDecimal totalPayrollReportingCurrency) {
    }
}

package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DepartmentSalaryExtremesReportResponse(
        LocalDate asOfDate,
        boolean includeInactive,
        String reportingCurrency,
        String rateBasis,
        int limitPerSide,
        List<DepartmentExtremes> departments) {

    public record DepartmentExtremes(
            String department,
            long headcount,
            EmployeeCompensationSnapshot highestPaid,
            EmployeeCompensationSnapshot lowestPaid,
            List<EmployeeCompensationSnapshot> highestPaidEmployees,
            List<EmployeeCompensationSnapshot> lowestPaidEmployees) {
    }

    public record EmployeeCompensationSnapshot(
            Long employeeId,
            String employeeNumber,
            String fullName,
            String countryCode,
            String jobTitle,
            String jobLevel,
            BigDecimal localAmount,
            String localCurrencyCode,
            BigDecimal reportingCurrencyAmount,
            LocalDate effectiveDate) {
    }
}

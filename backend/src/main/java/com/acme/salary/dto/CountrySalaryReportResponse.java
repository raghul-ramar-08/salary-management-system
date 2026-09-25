package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CountrySalaryReportResponse(
        LocalDate asOfDate,
        boolean includeInactive,
        String reportingCurrency,
        String rateBasis,
        Map<String, BigDecimal> exchangeRatesToReportingCurrency,
        long totalHeadcount,
        BigDecimal totalPayrollReportingCurrency,
        BigDecimal overallAverageReportingCurrency,
        BigDecimal overallMedianReportingCurrency,
        List<CountrySalaryMetrics> countries) {

    public record CountrySalaryMetrics(
            String countryCode,
            long headcount,
            String localCurrencyCode,
            BigDecimal averageSalaryLocal,
            BigDecimal medianSalaryLocal,
            BigDecimal totalPayrollLocal,
            BigDecimal averageSalaryReportingCurrency,
            BigDecimal medianSalaryReportingCurrency,
            BigDecimal totalPayrollReportingCurrency) {
    }
}

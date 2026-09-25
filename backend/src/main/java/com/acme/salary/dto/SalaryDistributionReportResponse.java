package com.acme.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalaryDistributionReportResponse(
        LocalDate asOfDate,
        boolean includeInactive,
        String reportingCurrency,
        String rateBasis,
        String countryCodeFilter,
        BigDecimal bandSize,
        long totalHeadcount,
        List<SalaryBandBucket> organizationBands,
        List<CountryBandDistribution> countryDistributions) {

    public record SalaryBandBucket(
            String label,
            BigDecimal minInclusive,
            BigDecimal maxExclusive,
            long headcount,
            BigDecimal percentageOfTotal) {
    }

    public record CountryBandDistribution(
            String countryCode,
            long totalHeadcount,
            List<SalaryBandBucket> bands) {
    }
}

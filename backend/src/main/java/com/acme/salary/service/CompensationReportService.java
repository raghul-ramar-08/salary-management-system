package com.acme.salary.service;

import com.acme.salary.dto.CountrySalaryReportResponse;
import com.acme.salary.dto.CountrySalaryReportResponse.CountrySalaryMetrics;
import com.acme.salary.dto.DepartmentSalaryExtremesReportResponse;
import com.acme.salary.dto.DepartmentSalaryExtremesReportResponse.DepartmentExtremes;
import com.acme.salary.dto.DepartmentSalaryExtremesReportResponse.EmployeeCompensationSnapshot;
import com.acme.salary.dto.DepartmentSalaryReportResponse;
import com.acme.salary.dto.DepartmentSalaryReportResponse.DepartmentSalaryMetrics;
import com.acme.salary.dto.SalaryDistributionReportResponse;
import com.acme.salary.dto.SalaryDistributionReportResponse.CountryBandDistribution;
import com.acme.salary.dto.SalaryDistributionReportResponse.SalaryBandBucket;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.SalaryRecordRepository;
import com.acme.salary.validation.SupportedCompensationCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompensationReportService {
    public static final String RATE_BASIS_DISCLOSURE =
            "Deterministic fixed conversion rates to USD (1 USD = 1.00 USD, 1 INR = 0.0120 USD, "
                    + "1 GBP = 1.2700 USD, 1 EUR = 1.0800 USD, 1 SGD = 0.7400 USD). "
                    + "By default, only ACTIVE employees with a salary period active on asOfDate are included.";

    private final SalaryRecordRepository salaryRecordRepository;

    public CompensationReportService(SalaryRecordRepository salaryRecordRepository) {
        this.salaryRecordRepository = salaryRecordRepository;
    }

    public CountrySalaryReportResponse countryReport(LocalDate asOfDate, boolean includeInactive) {
        LocalDate effectiveAsOf = asOfDate != null ? asOfDate : LocalDate.now();
        List<SalaryRecord> activeRecords = salaryRecordRepository.findActiveRecordsAsOf(effectiveAsOf, includeInactive);

        Map<String, List<SalaryRecord>> byCountry = activeRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getEmployee().getCountryCode().toUpperCase(Locale.ROOT)));

        List<CountrySalaryMetrics> countryMetrics = byCountry.entrySet().stream()
                .map(entry -> buildCountryMetrics(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CountrySalaryMetrics::totalPayrollReportingCurrency).reversed()
                        .thenComparing(CountrySalaryMetrics::countryCode))
                .toList();

        List<BigDecimal> allNormalizedAmounts = activeRecords.stream()
                .map(this::toReportingCurrency)
                .sorted()
                .toList();

        BigDecimal totalPayrollUsd = sum(allNormalizedAmounts);
        BigDecimal overallAverageUsd = average(allNormalizedAmounts);
        BigDecimal overallMedianUsd = median(allNormalizedAmounts);

        return new CountrySalaryReportResponse(
                effectiveAsOf,
                includeInactive,
                SupportedCompensationCatalog.REPORTING_CURRENCY,
                RATE_BASIS_DISCLOSURE,
                SupportedCompensationCatalog.FIXED_USD_RATES,
                activeRecords.size(),
                totalPayrollUsd,
                overallAverageUsd,
                overallMedianUsd,
                countryMetrics);
    }

    public DepartmentSalaryReportResponse departmentReport(LocalDate asOfDate, boolean includeInactive) {
        LocalDate effectiveAsOf = asOfDate != null ? asOfDate : LocalDate.now();
        List<SalaryRecord> activeRecords = salaryRecordRepository.findActiveRecordsAsOf(effectiveAsOf, includeInactive);

        Map<String, List<SalaryRecord>> byDepartment = activeRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getEmployee().getDepartment().trim()));

        List<DepartmentSalaryMetrics> departmentMetrics = byDepartment.entrySet().stream()
                .map(entry -> buildDepartmentMetrics(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DepartmentSalaryMetrics::totalPayrollReportingCurrency).reversed()
                        .thenComparing(DepartmentSalaryMetrics::department))
                .toList();

        List<BigDecimal> allNormalizedAmounts = activeRecords.stream()
                .map(this::toReportingCurrency)
                .sorted()
                .toList();

        return new DepartmentSalaryReportResponse(
                effectiveAsOf,
                includeInactive,
                SupportedCompensationCatalog.REPORTING_CURRENCY,
                RATE_BASIS_DISCLOSURE,
                SupportedCompensationCatalog.FIXED_USD_RATES,
                activeRecords.size(),
                sum(allNormalizedAmounts),
                departmentMetrics);
    }

    private DepartmentSalaryMetrics buildDepartmentMetrics(String department, List<SalaryRecord> records) {
        List<BigDecimal> normalizedAmounts = records.stream()
                .map(this::toReportingCurrency)
                .sorted()
                .toList();

        return new DepartmentSalaryMetrics(
                department,
                records.size(),
                average(normalizedAmounts),
                median(normalizedAmounts),
                sum(normalizedAmounts));
    }

    public SalaryDistributionReportResponse distributionReport(
            LocalDate asOfDate,
            boolean includeInactive,
            String countryCode,
            BigDecimal bandSize,
            List<BigDecimal> customThresholds) {
        LocalDate effectiveAsOf = asOfDate != null ? asOfDate : LocalDate.now();
        String normalizedCountry = (countryCode == null || countryCode.isBlank())
                ? null
                : SupportedCompensationCatalog.requireSupportedCountry(countryCode);

        BigDecimal effectiveBandSize = bandSize != null ? bandSize : new BigDecimal("25000.00");
        if (effectiveBandSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary band size must be greater than zero");
        }

        List<BandRange> bandRanges = buildBandRanges(effectiveBandSize, customThresholds);
        List<SalaryRecord> activeRecords = salaryRecordRepository.findActiveRecordsAsOf(effectiveAsOf, includeInactive);
        if (normalizedCountry != null) {
            activeRecords = activeRecords.stream()
                    .filter(sr -> normalizedCountry.equalsIgnoreCase(sr.getEmployee().getCountryCode()))
                    .toList();
        }

        List<BigDecimal> orgUsdAmounts = activeRecords.stream()
                .map(this::toReportingCurrency)
                .toList();
        List<SalaryBandBucket> organizationBands = bucketize(orgUsdAmounts, bandRanges);

        Map<String, List<SalaryRecord>> byCountry = activeRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getEmployee().getCountryCode().toUpperCase(Locale.ROOT)));

        List<CountryBandDistribution> countryDistributions = byCountry.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<BigDecimal> countryUsd = entry.getValue().stream()
                            .map(this::toReportingCurrency)
                            .toList();
                    return new CountryBandDistribution(
                            entry.getKey(),
                            countryUsd.size(),
                            bucketize(countryUsd, bandRanges));
                })
                .toList();

        return new SalaryDistributionReportResponse(
                effectiveAsOf,
                includeInactive,
                SupportedCompensationCatalog.REPORTING_CURRENCY,
                RATE_BASIS_DISCLOSURE,
                normalizedCountry,
                effectiveBandSize.setScale(2, RoundingMode.HALF_UP),
                activeRecords.size(),
                organizationBands,
                countryDistributions);
    }

    public DepartmentSalaryExtremesReportResponse departmentExtremesReport(
            LocalDate asOfDate,
            boolean includeInactive,
            String departmentFilter,
            int limitPerSide) {
        if (limitPerSide < 1 || limitPerSide > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 20");
        }
        LocalDate effectiveAsOf = asOfDate != null ? asOfDate : LocalDate.now();
        String trimmedFilter = (departmentFilter == null || departmentFilter.isBlank())
                ? null
                : departmentFilter.trim();

        List<SalaryRecord> activeRecords = salaryRecordRepository.findActiveRecordsAsOf(effectiveAsOf, includeInactive);
        if (trimmedFilter != null) {
            activeRecords = activeRecords.stream()
                    .filter(sr -> trimmedFilter.equalsIgnoreCase(sr.getEmployee().getDepartment().trim()))
                    .toList();
        }

        Map<String, List<SalaryRecord>> byDepartment = activeRecords.stream()
                .collect(Collectors.groupingBy(sr -> sr.getEmployee().getDepartment().trim()));

        Comparator<EmployeeCompensationSnapshot> byPayAscending = Comparator
                .comparing(EmployeeCompensationSnapshot::reportingCurrencyAmount)
                .thenComparing(EmployeeCompensationSnapshot::employeeNumber);

        List<DepartmentExtremes> departments = byDepartment.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<EmployeeCompensationSnapshot> sortedAscending = entry.getValue().stream()
                            .map(this::toSnapshot)
                            .sorted(byPayAscending)
                            .toList();

                    List<EmployeeCompensationSnapshot> lowest = sortedAscending.stream()
                            .limit(limitPerSide)
                            .toList();
                    List<EmployeeCompensationSnapshot> highest = sortedAscending.stream()
                            .sorted(byPayAscending.reversed())
                            .limit(limitPerSide)
                            .toList();

                    return new DepartmentExtremes(
                            entry.getKey(),
                            sortedAscending.size(),
                            highest.isEmpty() ? null : highest.get(0),
                            lowest.isEmpty() ? null : lowest.get(0),
                            highest,
                            lowest);
                })
                .toList();

        return new DepartmentSalaryExtremesReportResponse(
                effectiveAsOf,
                includeInactive,
                SupportedCompensationCatalog.REPORTING_CURRENCY,
                RATE_BASIS_DISCLOSURE,
                limitPerSide,
                departments);
    }

    private EmployeeCompensationSnapshot toSnapshot(SalaryRecord record) {
        var employee = record.getEmployee();
        return new EmployeeCompensationSnapshot(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getCountryCode(),
                employee.getJobTitle(),
                employee.getJobLevel(),
                record.getAmount(),
                record.getCurrencyCode(),
                toReportingCurrency(record),
                record.getEffectiveDate());
    }

    private List<BandRange> buildBandRanges(BigDecimal bandSize, List<BigDecimal> customThresholds) {
        if (customThresholds != null && !customThresholds.isEmpty()) {
            List<BigDecimal> sorted = customThresholds.stream()
                    .map(v -> v.setScale(2, RoundingMode.HALF_UP))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Salary band thresholds must be non-negative");
                }
                if (i > 0 && sorted.get(i).compareTo(sorted.get(i - 1)) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Salary band thresholds must be strictly increasing");
                }
            }
            java.util.ArrayList<BandRange> ranges = new java.util.ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                BigDecimal min = sorted.get(i);
                BigDecimal max = (i + 1 < sorted.size()) ? sorted.get(i + 1) : null;
                ranges.add(new BandRange(formatBandLabel(min, max), min, max));
            }
            return ranges;
        }

        java.util.ArrayList<BandRange> ranges = new java.util.ArrayList<>();
        int bucketCount = 5;
        BigDecimal scaledStep = bandSize.setScale(2, RoundingMode.HALF_UP);
        for (int i = 0; i < bucketCount; i++) {
            BigDecimal min = scaledStep.multiply(BigDecimal.valueOf(i));
            BigDecimal max = scaledStep.multiply(BigDecimal.valueOf(i + 1));
            ranges.add(new BandRange(formatBandLabel(min, max), min, max));
        }
        BigDecimal topMin = scaledStep.multiply(BigDecimal.valueOf(bucketCount));
        ranges.add(new BandRange(formatBandLabel(topMin, null), topMin, null));
        return ranges;
    }

    private List<SalaryBandBucket> bucketize(List<BigDecimal> amounts, List<BandRange> bandRanges) {
        long total = amounts.size();
        return bandRanges.stream()
                .map(range -> {
                    long count = amounts.stream()
                            .filter(amount -> amount.compareTo(range.minInclusive()) >= 0
                                    && (range.maxExclusive() == null || amount.compareTo(range.maxExclusive()) < 0))
                            .count();
                    BigDecimal pct = total == 0
                            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(count * 100)
                                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                    return new SalaryBandBucket(
                            range.label(),
                            range.minInclusive(),
                            range.maxExclusive(),
                            count,
                            pct);
                })
                .toList();
    }

    private String formatBandLabel(BigDecimal min, BigDecimal max) {
        long minVal = min.longValue();
        if (minVal > 0) {
            minVal = minVal + 1;
        }
        if (max == null) {
            return "$%,d+".formatted(minVal);
        }
        return "$%,d – $%,d".formatted(minVal, max.longValue());
    }

    private record BandRange(String label, BigDecimal minInclusive, BigDecimal maxExclusive) {
    }

    private CountrySalaryMetrics buildCountryMetrics(String countryCode, List<SalaryRecord> records) {
        List<BigDecimal> normalizedAmounts = records.stream()
                .map(this::toReportingCurrency)
                .sorted()
                .toList();

        Set<String> currencies = records.stream()
                .map(sr -> sr.getCurrencyCode().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        String localCurrency = currencies.size() == 1 ? currencies.iterator().next() : "MIXED";
        BigDecimal avgLocal = null;
        BigDecimal medianLocal = null;
        BigDecimal totalLocal = null;

        if (currencies.size() == 1) {
            List<BigDecimal> localAmounts = records.stream()
                    .map(SalaryRecord::getAmount)
                    .sorted()
                    .toList();
            avgLocal = average(localAmounts);
            medianLocal = median(localAmounts);
            totalLocal = sum(localAmounts);
        }

        return new CountrySalaryMetrics(
                countryCode,
                records.size(),
                localCurrency,
                avgLocal,
                medianLocal,
                totalLocal,
                average(normalizedAmounts),
                median(normalizedAmounts),
                sum(normalizedAmounts));
    }

    public BigDecimal toReportingCurrency(SalaryRecord record) {
        String currency = record.getCurrencyCode().trim().toUpperCase(Locale.ROOT);
        BigDecimal rate = SupportedCompensationCatalog.FIXED_USD_RATES.get(currency);
        if (rate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No deterministic FX conversion rate configured for currency: " + currency);
        }
        return record.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal sum(List<BigDecimal> sortedAmounts) {
        return sortedAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal average(List<BigDecimal> sortedAmounts) {
        if (sortedAmounts.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum(sortedAmounts)
                .divide(BigDecimal.valueOf(sortedAmounts.size()), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal median(List<BigDecimal> sortedAmounts) {
        if (sortedAmounts.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int size = sortedAmounts.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sortedAmounts.get(middle).setScale(2, RoundingMode.HALF_UP);
        }
        return sortedAmounts.get(middle - 1)
                .add(sortedAmounts.get(middle))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}

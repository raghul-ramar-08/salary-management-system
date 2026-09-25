package com.acme.salary.service;

import com.acme.salary.dto.CountrySalaryReportResponse;
import com.acme.salary.dto.CountrySalaryReportResponse.CountrySalaryMetrics;
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

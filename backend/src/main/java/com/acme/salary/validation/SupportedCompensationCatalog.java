package com.acme.salary.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SupportedCompensationCatalog {
    public static final Set<String> SUPPORTED_COUNTRIES = Set.of("US", "IN", "GB", "DE", "SG");
    public static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "INR", "GBP", "EUR", "SGD");

    /**
     * Deterministic fixed conversion rates to USD (1 unit of currency = X USD)
     * used for normalized cross-country compensation reporting.
     */
    public static final String REPORTING_CURRENCY = "USD";
    public static final Map<String, BigDecimal> FIXED_USD_RATES = Map.of(
            "USD", new BigDecimal("1.0000"),
            "INR", new BigDecimal("0.0120"),
            "GBP", new BigDecimal("1.2700"),
            "EUR", new BigDecimal("1.0800"),
            "SGD", new BigDecimal("0.7400"));

    private SupportedCompensationCatalog() {
    }

    public static String requireSupportedCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country code is required");
        }
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_COUNTRIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported country code: " + normalized + ". Supported countries: " + SUPPORTED_COUNTRIES);
        }
        return normalized;
    }

    public static String requireSupportedCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency code is required");
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported currency code: " + normalized + ". Supported currencies: " + SUPPORTED_CURRENCIES);
        }
        return normalized;
    }

    public static BigDecimal requirePositiveSalary(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary amount must be greater than zero");
        }
        return amount;
    }
}

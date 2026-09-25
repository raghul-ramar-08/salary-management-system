package com.acme.salary.validation;

import com.acme.salary.dto.CreateEmployeeRequest;
import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import com.acme.salary.service.EmployeeService;
import com.acme.salary.service.SalaryRecordService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestValidationTest {
    private static Validator validator;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @InjectMocks
    private SalaryRecordService salaryRecordService;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidEmployeeAndSalaryRequest() {
        var request = new CreateEmployeeRequest(
                "ACME-10002",
                "Jordan",
                "Patel",
                "US",
                "Engineering",
                "Software Engineer",
                "L3",
                LocalDate.of(2024, 2, 1),
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        new BigDecimal("110000.00"),
                        "USD",
                        LocalDate.of(2024, 2, 1),
                        "Initial compensation"));

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("US", SupportedCompensationCatalog.requireSupportedCountry("us"));
        assertEquals("USD", SupportedCompensationCatalog.requireSupportedCurrency("usd"));
        assertEquals(new BigDecimal("110000.00"),
                SupportedCompensationCatalog.requirePositiveSalary(new BigDecimal("110000.00")));
    }

    @Test
    void rejectsMissingRequiredEmployeeAndSalaryFields() {
        var invalidRequest = new CreateEmployeeRequest(
                "",
                " ",
                "",
                "",
                "",
                "",
                null,
                null,
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        null,
                        "",
                        null,
                        ""));

        var violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("employeeNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfJoining")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("initialSalary.amount")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("initialSalary.changeReason")));
    }

    @Test
    void rejectsNonPositiveSalaryAmounts() {
        var zeroSalary = new SalaryRecordRequest(
                BigDecimal.ZERO, "USD", LocalDate.now(), "Adjustment");
        var negativeSalary = new SalaryRecordRequest(
                new BigDecimal("-500.00"), "USD", LocalDate.now(), "Adjustment");

        assertFalse(validator.validate(zeroSalary).isEmpty());
        assertFalse(validator.validate(negativeSalary).isEmpty());

        var zeroError = assertThrows(ResponseStatusException.class,
                () -> SupportedCompensationCatalog.requirePositiveSalary(BigDecimal.ZERO));
        assertEquals(HttpStatus.BAD_REQUEST, zeroError.getStatusCode());

        var negativeError = assertThrows(ResponseStatusException.class,
                () -> salaryRecordService.addRecord("ACME-00001", negativeSalary));
        assertEquals(HttpStatus.BAD_REQUEST, negativeError.getStatusCode());
    }

    @Test
    void rejectsUnsupportedCountryAndCurrencyValues() {
        var unsupportedCountryRequest = new CreateEmployeeRequest(
                "ACME-10005",
                "Alex",
                "Kim",
                "ZZ",
                "Finance",
                "Analyst",
                "L2",
                LocalDate.now(),
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        new BigDecimal("85000.00"),
                        "USD",
                        LocalDate.now(),
                        "Initial hire"));

        when(employeeRepository.existsByEmployeeNumberIgnoreCase("ACME-10005")).thenReturn(false);

        var countryError = assertThrows(ResponseStatusException.class,
                () -> employeeService.create(unsupportedCountryRequest));
        assertEquals(HttpStatus.BAD_REQUEST, countryError.getStatusCode());

        var unsupportedCurrencySalary = new SalaryRecordRequest(
                new BigDecimal("85000.00"),
                "XYZ",
                LocalDate.now(),
                "Annual review");

        var currencyError = assertThrows(ResponseStatusException.class,
                () -> salaryRecordService.addRecord("ACME-00001", unsupportedCurrencySalary));
        assertEquals(HttpStatus.BAD_REQUEST, currencyError.getStatusCode());
    }

    @Test
    void rejectsDuplicateEmployeeNumberWithConflictStatus() {
        var duplicateRequest = new CreateEmployeeRequest(
                "ACME-00001",
                "Avery",
                "Shah",
                "US",
                "Engineering",
                "Engineer",
                "L2",
                LocalDate.now(),
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        new BigDecimal("95000.00"),
                        "USD",
                        LocalDate.now(),
                        "Initial hire"));

        when(employeeRepository.existsByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(true);

        var conflictError = assertThrows(ResponseStatusException.class,
                () -> employeeService.create(duplicateRequest));
        assertEquals(HttpStatus.CONFLICT, conflictError.getStatusCode());
    }
}

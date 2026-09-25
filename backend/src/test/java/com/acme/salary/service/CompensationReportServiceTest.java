package com.acme.salary.service;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationReportServiceTest {
    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private CompensationReportService reportService;

    @Test
    void countryReportCalculatesAverageMedianHeadcountAndConvertedPayroll() {
        LocalDate asOfDate = LocalDate.of(2026, 9, 26);

        Employee us1 = new Employee("ACME-00001", "Avery", "Shah", "US", "Engineering",
                "Engineer", "L2", LocalDate.of(2022, 1, 1), EmploymentStatus.ACTIVE);
        Employee us2 = new Employee("ACME-00002", "Jordan", "Patel", "US", "Engineering",
                "Senior Engineer", "L4", LocalDate.of(2021, 1, 1), EmploymentStatus.ACTIVE);
        Employee us3 = new Employee("ACME-00003", "Morgan", "Kim", "US", "Product",
                "Director", "L6", LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE);

        Employee in1 = new Employee("ACME-00004", "Priya", "Nair", "IN", "Engineering",
                "Engineer", "L2", LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE);
        Employee in2 = new Employee("ACME-00005", "Sam", "Kumar", "IN", "Finance",
                "Analyst", "L1", LocalDate.of(2023, 6, 1), EmploymentStatus.ACTIVE);

        List<SalaryRecord> activeRecords = List.of(
                new SalaryRecord(us1, new BigDecimal("80000.00"), "USD", asOfDate.minusMonths(6), null, "Hire"),
                new SalaryRecord(us2, new BigDecimal("100000.00"), "USD", asOfDate.minusMonths(6), null, "Hire"),
                new SalaryRecord(us3, new BigDecimal("150000.00"), "USD", asOfDate.minusMonths(6), null, "Hire"),
                // 1,000,000 INR * 0.0120 = 12,000.00 USD
                new SalaryRecord(in1, new BigDecimal("1000000.00"), "INR", asOfDate.minusMonths(6), null, "Hire"),
                // 2,000,000 INR * 0.0120 = 24,000.00 USD
                new SalaryRecord(in2, new BigDecimal("2000000.00"), "INR", asOfDate.minusMonths(6), null, "Hire"));

        when(salaryRecordRepository.findActiveRecordsAsOf(asOfDate, false)).thenReturn(activeRecords);

        var report = reportService.countryReport(asOfDate, false);

        assertEquals(5, report.totalHeadcount());
        assertEquals("USD", report.reportingCurrency());
        // Total USD payroll = 80000 + 100000 + 150000 + 12000 + 24000 = 366000.00
        assertEquals(new BigDecimal("366000.00"), report.totalPayrollReportingCurrency());
        // Sorted USD amounts: [12000, 24000, 80000, 100000, 150000] -> median = 80000.00, avg = 73200.00
        assertEquals(new BigDecimal("73200.00"), report.overallAverageReportingCurrency());
        assertEquals(new BigDecimal("80000.00"), report.overallMedianReportingCurrency());

        assertEquals(2, report.countries().size());
        var usMetrics = report.countries().get(0);
        assertEquals("US", usMetrics.countryCode());
        assertEquals(3, usMetrics.headcount());
        assertEquals(new BigDecimal("110000.00"), usMetrics.averageSalaryReportingCurrency());
        assertEquals(new BigDecimal("100000.00"), usMetrics.medianSalaryReportingCurrency());
        assertEquals(new BigDecimal("330000.00"), usMetrics.totalPayrollReportingCurrency());

        var inMetrics = report.countries().get(1);
        assertEquals("IN", inMetrics.countryCode());
        assertEquals(2, inMetrics.headcount());
        assertEquals("INR", inMetrics.localCurrencyCode());
        assertEquals(new BigDecimal("1500000.00"), inMetrics.averageSalaryLocal());
        assertEquals(new BigDecimal("1500000.00"), inMetrics.medianSalaryLocal());
        assertEquals(new BigDecimal("3000000.00"), inMetrics.totalPayrollLocal());
        assertEquals(new BigDecimal("18000.00"), inMetrics.averageSalaryReportingCurrency());
        assertEquals(new BigDecimal("18000.00"), inMetrics.medianSalaryReportingCurrency());
        assertEquals(new BigDecimal("36000.00"), inMetrics.totalPayrollReportingCurrency());
    }
}

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

    @Test
    void departmentReportNormalizesMultiCurrencyDepartmentsAndCalculatesMedianAndPayroll() {
        LocalDate asOfDate = LocalDate.of(2026, 9, 26);

        Employee engUs = new Employee("ACME-00001", "Avery", "Shah", "US", "Engineering",
                "Engineer", "L3", LocalDate.of(2022, 1, 1), EmploymentStatus.ACTIVE);
        Employee engGb = new Employee("ACME-00002", "Jordan", "Patel", "GB", "Engineering",
                "Engineer", "L4", LocalDate.of(2021, 1, 1), EmploymentStatus.ACTIVE);
        Employee engDe = new Employee("ACME-00003", "Morgan", "Kim", "DE", "Engineering",
                "Lead", "L5", LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE);
        Employee salesSg = new Employee("ACME-00004", "Sam", "Kumar", "SG", "Sales",
                "Specialist", "L2", LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE);

        List<SalaryRecord> activeRecords = List.of(
                // 100,000 USD -> 100,000.00 USD
                new SalaryRecord(engUs, new BigDecimal("100000.00"), "USD", asOfDate.minusMonths(2), null, "Base"),
                // 50,000 GBP * 1.2700 = 63,500.00 USD
                new SalaryRecord(engGb, new BigDecimal("50000.00"), "GBP", asOfDate.minusMonths(2), null, "Base"),
                // 75,000 EUR * 1.0800 = 81,000.00 USD
                new SalaryRecord(engDe, new BigDecimal("75000.00"), "EUR", asOfDate.minusMonths(2), null, "Base"),
                // 80,000 SGD * 0.7400 = 59,200.00 USD
                new SalaryRecord(salesSg, new BigDecimal("80000.00"), "SGD", asOfDate.minusMonths(2), null, "Base"));

        when(salaryRecordRepository.findActiveRecordsAsOf(asOfDate, false)).thenReturn(activeRecords);

        var report = reportService.departmentReport(asOfDate, false);

        assertEquals(4, report.totalHeadcount());
        // Total payroll = 100,000 + 63,500 + 81,000 + 59,200 = 303,700.00 USD
        assertEquals(new BigDecimal("303700.00"), report.totalPayrollReportingCurrency());
        assertEquals(2, report.departments().size());

        var engineering = report.departments().get(0);
        assertEquals("Engineering", engineering.department());
        assertEquals(3, engineering.headcount());
        // Sorted USD for Engineering: [63,500.00, 81,000.00, 100,000.00]
        // Total = 244,500.00, Average = 81,500.00, Median = 81,000.00
        assertEquals(new BigDecimal("244500.00"), engineering.totalPayrollReportingCurrency());
        assertEquals(new BigDecimal("81500.00"), engineering.averageSalaryReportingCurrency());
        assertEquals(new BigDecimal("81000.00"), engineering.medianSalaryReportingCurrency());

        var sales = report.departments().get(1);
        assertEquals("Sales", sales.department());
        assertEquals(1, sales.headcount());
        assertEquals(new BigDecimal("59200.00"), sales.totalPayrollReportingCurrency());
    }

    @Test
    void distributionReportBucketsOrgAndCountrySalariesIntoConfigurableBands() {
        LocalDate asOfDate = LocalDate.of(2026, 9, 26);

        Employee in1 = new Employee("ACME-00001", "Priya", "Nair", "IN", "Engineering",
                "Engineer", "L1", LocalDate.of(2023, 1, 1), EmploymentStatus.ACTIVE);
        Employee us1 = new Employee("ACME-00002", "Avery", "Shah", "US", "Engineering",
                "Engineer", "L3", LocalDate.of(2022, 1, 1), EmploymentStatus.ACTIVE);
        Employee us2 = new Employee("ACME-00003", "Jordan", "Patel", "US", "Product",
                "Director", "L6", LocalDate.of(2020, 1, 1), EmploymentStatus.ACTIVE);

        List<SalaryRecord> activeRecords = List.of(
                // 1,000,000 INR * 0.0120 = 12,000.00 USD -> [0, 30000)
                new SalaryRecord(in1, new BigDecimal("1000000.00"), "INR", asOfDate.minusMonths(3), null, "Base"),
                // 85,000.00 USD -> [60000, 90000)
                new SalaryRecord(us1, new BigDecimal("85000.00"), "USD", asOfDate.minusMonths(3), null, "Base"),
                // 160,000.00 USD -> [150000, null)
                new SalaryRecord(us2, new BigDecimal("160000.00"), "USD", asOfDate.minusMonths(3), null, "Base"));

        when(salaryRecordRepository.findActiveRecordsAsOf(asOfDate, false)).thenReturn(activeRecords);

        var report = reportService.distributionReport(
                asOfDate, false, null, new BigDecimal("30000"), null);

        assertEquals(3, report.totalHeadcount());
        assertEquals(6, report.organizationBands().size());
        assertEquals(1, report.organizationBands().get(0).headcount()); // $0 - $30,000
        assertEquals(1, report.organizationBands().get(2).headcount()); // $60,000 - $90,000
        assertEquals(1, report.organizationBands().get(5).headcount()); // $150,000+

        assertEquals(2, report.countryDistributions().size());
        var inDistribution = report.countryDistributions().get(0);
        assertEquals("IN", inDistribution.countryCode());
        assertEquals(1, inDistribution.totalHeadcount());
        assertEquals(1, inDistribution.bands().get(0).headcount());

        var usDistribution = report.countryDistributions().get(1);
        assertEquals("US", usDistribution.countryCode());
        assertEquals(2, usDistribution.totalHeadcount());
        assertEquals(1, usDistribution.bands().get(2).headcount());
        assertEquals(1, usDistribution.bands().get(5).headcount());
    }
}

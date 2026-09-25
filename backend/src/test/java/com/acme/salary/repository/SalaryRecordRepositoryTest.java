package com.acme.salary.repository;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.entity.SalaryRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class SalaryRecordRepositoryTest {
    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private SalaryRecordRepository salaryRecords;

    @Test
    void currentOnlySearchReturnsTheRecordActiveOnTheRequestedDate() {
        Employee employee = employees.saveAndFlush(new Employee(
                "ACME-00001", "Avery", "Shah", "US", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE));
        salaryRecords.saveAllAndFlush(List.of(
                new SalaryRecord(employee, new BigDecimal("80000.00"), "USD",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 29), "Previous salary"),
                new SalaryRecord(employee, new BigDecimal("90000.00"), "USD",
                        LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31), "Current salary"),
                new SalaryRecord(employee, new BigDecimal("100000.00"), "USD",
                        LocalDate.of(2027, 1, 1), null, "Future salary")));

        var current = salaryRecords.search(null, null, null, null, null, null,
                true, LocalDate.of(2026, 6, 30), PageRequest.of(0, 25));
        var allHistory = salaryRecords.search(null, null, null, null, null, null,
                false, LocalDate.of(2026, 6, 30), PageRequest.of(0, 25));

        assertEquals(1, current.getTotalElements());
        assertEquals(new BigDecimal("90000.00"), current.getContent().get(0).getAmount());
        assertEquals(3, allHistory.getTotalElements());
    }

    @Test
    void findActiveRecordsAsOfFiltersByDateAndEmploymentStatus() {
        Employee activeEmployee = employees.saveAndFlush(new Employee(
                "ACME-00010", "Avery", "Shah", "US", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE));
        Employee inactiveEmployee = employees.saveAndFlush(new Employee(
                "ACME-00011", "Jordan", "Patel", "IN", "Finance",
                "Analyst", "L1", LocalDate.of(2021, 1, 1), EmploymentStatus.INACTIVE));

        salaryRecords.saveAllAndFlush(List.of(
                new SalaryRecord(activeEmployee, new BigDecimal("80000.00"), "USD",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2026, 5, 31), "Superseded salary"),
                new SalaryRecord(activeEmployee, new BigDecimal("95000.00"), "USD",
                        LocalDate.of(2026, 6, 1), null, "Active salary"),
                new SalaryRecord(inactiveEmployee, new BigDecimal("900000.00"), "INR",
                        LocalDate.of(2025, 1, 1), null, "Inactive employee salary")));

        var activeOnly = salaryRecords.findActiveRecordsAsOf(LocalDate.of(2026, 6, 15), false);
        var withInactive = salaryRecords.findActiveRecordsAsOf(LocalDate.of(2026, 6, 15), true);

        assertEquals(1, activeOnly.size());
        assertEquals("ACME-00010", activeOnly.get(0).getEmployee().getEmployeeNumber());
        assertEquals(new BigDecimal("95000.00"), activeOnly.get(0).getAmount());
        assertEquals(2, withInactive.size());
    }
}

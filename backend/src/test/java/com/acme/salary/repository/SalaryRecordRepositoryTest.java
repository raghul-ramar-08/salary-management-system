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
}

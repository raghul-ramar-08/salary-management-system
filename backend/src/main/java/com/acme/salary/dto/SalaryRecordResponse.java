package com.acme.salary.dto;

import com.acme.salary.entity.SalaryRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SalaryRecordResponse(Long id, String employeeNumber, String employeeName,
                                   String countryCode, String department, BigDecimal amount,
                                   String currencyCode, LocalDate effectiveDate,
                                   LocalDate effectiveTo, String changeReason, Instant recordedAt) {
    public static SalaryRecordResponse from(SalaryRecord record) {
        var employee = record.getEmployee();
        return new SalaryRecordResponse(record.getId(), employee.getEmployeeNumber(),
                employee.getFirstName() + " " + employee.getLastName(), employee.getCountryCode(),
                employee.getDepartment(), record.getAmount(), record.getCurrencyCode(),
                record.getEffectiveDate(), record.getEffectiveTo(), record.getChangeReason(), record.getRecordedAt());
    }
}

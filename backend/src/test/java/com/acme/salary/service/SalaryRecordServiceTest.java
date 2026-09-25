package com.acme.salary.service;

import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.entity.Employee;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalaryRecordServiceTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final SalaryRecordRepository records = mock(SalaryRecordRepository.class);
    private final SalaryRecordService service = new SalaryRecordService(employees, records);

    @Test
    void rejectsSalaryHistoryForUnknownEmployee() {
        when(employees.existsByEmployeeNumberIgnoreCase("ACME-99999")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.history(" acme-99999 "));

        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void rejectsSecondRecordForSameEffectiveDate() {
        Employee employee = mock(Employee.class);
        when(employees.findByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(Optional.of(employee));
        when(employee.getId()).thenReturn(1L);
        LocalDate effectiveDate = LocalDate.of(2026, 1, 1);
        when(records.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addRecord("ACME-00001",
                        new SalaryRecordRequest(java.math.BigDecimal.valueOf(1000), "USD",
                                effectiveDate, "Annual review")));

        assertEquals(409, error.getStatusCode().value());
        verify(records).existsByEmployee_IdAndEffectiveDate(1L, effectiveDate);
    }

    @Test
    void addingLaterSalaryClosesPreviousRecordTheDayBefore() {
        Employee employee = employee();
        LocalDate oldEffectiveDate = LocalDate.of(2025, 1, 1);
        SalaryRecord previous = new SalaryRecord(employee, new BigDecimal("90000.00"), "USD",
                oldEffectiveDate, null, "Initial salary");
        LocalDate newEffectiveDate = LocalDate.of(2026, 4, 1);

        when(employees.findByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(Optional.of(employee));
        when(records.existsByEmployee_IdAndEffectiveDate(1L, newEffectiveDate)).thenReturn(false);
        when(records.findByEmployee_IdOrderByEffectiveDateAsc(1L)).thenReturn(List.of(previous));
        when(records.save(any(SalaryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.addRecord(" acme-00001 ", new SalaryRecordRequest(
                new BigDecimal("100000.00"), "usd", newEffectiveDate, "  Promotion  "));

        assertEquals(newEffectiveDate.minusDays(1), previous.getEffectiveTo());
        assertEquals(newEffectiveDate, response.effectiveDate());
        assertNull(response.effectiveTo());
        assertEquals("USD", response.currencyCode());
        assertEquals("Promotion", response.changeReason());
    }

    @Test
    void backdatedSalaryEndsBeforeTheNextScheduledRecord() {
        Employee employee = employee();
        LocalDate nextEffectiveDate = LocalDate.of(2027, 1, 1);
        SalaryRecord next = new SalaryRecord(employee, new BigDecimal("120000.00"), "USD",
                nextEffectiveDate, null, "Scheduled increase");
        LocalDate insertedEffectiveDate = LocalDate.of(2026, 6, 15);

        when(employees.findByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(Optional.of(employee));
        when(records.existsByEmployee_IdAndEffectiveDate(1L, insertedEffectiveDate)).thenReturn(false);
        when(records.findByEmployee_IdOrderByEffectiveDateAsc(1L)).thenReturn(List.of(next));
        when(records.save(any(SalaryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.addRecord("ACME-00001", new SalaryRecordRequest(
                new BigDecimal("110000.00"), "USD", insertedEffectiveDate, "Market adjustment"));

        assertEquals(nextEffectiveDate.minusDays(1), response.effectiveTo());
        assertNull(next.getEffectiveTo());
    }

    @Test
    void rejectsSearchWhenEffectiveDateRangeIsReversed() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 6, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.search(null, null, null, null, from, to, false,
                        org.springframework.data.domain.PageRequest.of(0, 25)));

        assertEquals(400, error.getStatusCode().value());
    }

    private Employee employee() {
        Employee employee = mock(Employee.class);
        when(employee.getId()).thenReturn(1L);
        when(employee.getEmployeeNumber()).thenReturn("ACME-00001");
        when(employee.getFirstName()).thenReturn("Avery");
        when(employee.getLastName()).thenReturn("Shah");
        when(employee.getCountryCode()).thenReturn("US");
        when(employee.getDepartment()).thenReturn("Engineering");
        return employee;
    }
}

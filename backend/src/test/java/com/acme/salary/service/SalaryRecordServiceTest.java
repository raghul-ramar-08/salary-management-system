package com.acme.salary.service;

import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.entity.Employee;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}

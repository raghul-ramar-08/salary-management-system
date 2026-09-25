package com.acme.salary.service;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void trimsSearchFiltersAndMapsEmployeeToResponse() {
        var pageable = PageRequest.of(0, 25, Sort.by("lastName"));
        var employee = new Employee(
                "ACME-00001", "Avery", "Shah", "IN", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE);
        when(employeeRepository.search("Avery", "IN", "Engineering", EmploymentStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = employeeService.search(" Avery ", " in ", " Engineering ", EmploymentStatus.ACTIVE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("ACME-00001", result.getContent().get(0).employeeNumber());
        assertEquals("Avery", result.getContent().get(0).firstName());
        verify(employeeRepository).search("Avery", "IN", "Engineering", EmploymentStatus.ACTIVE, pageable);
    }

    @Test
    void convertsBlankFiltersToNull() {
        var pageable = PageRequest.of(0, 25);
        when(employeeRepository.search(isNull(), isNull(), isNull(), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = employeeService.search("  ", " ", null, null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(employeeRepository).search(null, null, null, null, pageable);
    }
}

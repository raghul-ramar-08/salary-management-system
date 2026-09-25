package com.acme.salary.service;

import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Page<EmployeeResponse> search(String query, String countryCode, String department,
                                         EmploymentStatus status, Pageable pageable) {
        String normalizedQuery = blankToNull(query);
        String normalizedCountry = blankToNull(countryCode);
        if (normalizedCountry != null) {
            normalizedCountry = normalizedCountry.toUpperCase(Locale.ROOT);
        }

        return employeeRepository.search(normalizedQuery, normalizedCountry,
                        blankToNull(department), status, pageable)
                .map(EmployeeResponse::from);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

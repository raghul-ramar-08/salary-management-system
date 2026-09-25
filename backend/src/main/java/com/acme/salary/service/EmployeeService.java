package com.acme.salary.service;

import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.EmployeeProfileResponse;
import com.acme.salary.dto.SalaryRecordResponse;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
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

    public EmployeeProfileResponse profile(Long employeeId) {
        var employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        List<SalaryRecordResponse> history = salaryRecordRepository
                .findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(employeeId)
                .stream().map(SalaryRecordResponse::from).toList();
        LocalDate today = LocalDate.now();
        SalaryRecordResponse currentSalary = history.stream()
                .filter(record -> !record.effectiveDate().isAfter(today))
                .filter(record -> record.effectiveTo() == null || !record.effectiveTo().isBefore(today))
                .findFirst().orElse(null);

        return new EmployeeProfileResponse(EmployeeResponse.from(employee), currentSalary, history);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

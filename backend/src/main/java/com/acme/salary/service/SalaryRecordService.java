package com.acme.salary.service;

import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.dto.SalaryRecordResponse;
import com.acme.salary.entity.Employee;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class SalaryRecordService {
    private static final Logger log = LoggerFactory.getLogger(SalaryRecordService.class);
    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public SalaryRecordService(EmployeeRepository employeeRepository,
                               SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    public List<SalaryRecordResponse> history(String employeeNumber) {
        String normalizedNumber = normalizeEmployeeNumber(employeeNumber);
        if (!employeeRepository.existsByEmployeeNumberIgnoreCase(normalizedNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        List<SalaryRecordResponse> history = salaryRecordRepository
                .findByEmployee_EmployeeNumberIgnoreCaseOrderByEffectiveDateDescRecordedAtDesc(normalizedNumber)
                .stream().map(SalaryRecordResponse::from).toList();
        log.debug("Salary history retrieved: employeeNumber={}, records={}", normalizedNumber, history.size());
        return history;
    }

    public Page<SalaryRecordResponse> search(String employeeNumber, String countryCode,
                                             String department, String currencyCode,
                                             LocalDate effectiveFrom, LocalDate effectiveTo,
                                             boolean currentOnly,
                                             Pageable pageable) {
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Effective date start must be on or before the end date");
        }

        String normalizedEmployeeNumber = blankToNull(employeeNumber);
        String normalizedCountryCode = uppercaseOrNull(countryCode);
        String normalizedDepartment = blankToNull(department);
        String normalizedCurrencyCode = uppercaseOrNull(currencyCode);
        log.debug("Salary record search started: page={}, size={}, filtersApplied={}",
                pageable.getPageNumber(), pageable.getPageSize(),
                normalizedEmployeeNumber != null || normalizedCountryCode != null
                        || normalizedDepartment != null || normalizedCurrencyCode != null
                        || effectiveFrom != null || effectiveTo != null || currentOnly);

        Page<SalaryRecordResponse> results = salaryRecordRepository.search(
                        normalizedEmployeeNumber, normalizedCountryCode, normalizedDepartment,
                        normalizedCurrencyCode, effectiveFrom, effectiveTo, currentOnly,
                        LocalDate.now(), pageable)
                .map(SalaryRecordResponse::from);
        log.debug("Salary record search completed: returned={}, total={}",
                results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    @Transactional
    public SalaryRecordResponse addRecord(String employeeNumber, SalaryRecordRequest request) {
        String normalizedNumber = normalizeEmployeeNumber(employeeNumber);
        Employee employee = employeeRepository.findByEmployeeNumberIgnoreCase(normalizedNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if (salaryRecordRepository.existsByEmployee_IdAndEffectiveDate(employee.getId(), request.effectiveDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A salary record already exists for this effective date");
        }

        List<SalaryRecord> history = salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateAsc(employee.getId());
        SalaryRecord previous = history.stream()
                .filter(existing -> existing.getEffectiveDate().isBefore(request.effectiveDate()))
                .max(Comparator.comparing(SalaryRecord::getEffectiveDate)).orElse(null);
        SalaryRecord next = history.stream()
                .filter(existing -> existing.getEffectiveDate().isAfter(request.effectiveDate()))
                .min(Comparator.comparing(SalaryRecord::getEffectiveDate)).orElse(null);

        if (previous != null) previous.setEffectiveTo(request.effectiveDate().minusDays(1));
        LocalDate effectiveTo = next == null ? null : next.getEffectiveDate().minusDays(1);
        SalaryRecord record = new SalaryRecord(employee, request.amount(),
                request.currencyCode().toUpperCase(Locale.ROOT), request.effectiveDate(), effectiveTo,
                request.changeReason().trim());
        SalaryRecord saved = salaryRecordRepository.save(record);
        // Salary amounts are deliberately excluded from logs because compensation is sensitive data.
        log.info("Salary record created: recordId={}, employeeNumber={}, effectiveDate={}",
                saved.getId(), normalizedNumber, saved.getEffectiveDate());
        return SalaryRecordResponse.from(saved);
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        return employeeNumber.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String uppercaseOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}

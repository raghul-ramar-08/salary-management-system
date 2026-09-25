package com.acme.salary.controller;

import com.acme.salary.dto.SalaryRecordResponse;
import com.acme.salary.service.SalaryRecordService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/salary-records")
@Validated
public class SalaryRecordSearchController {
    private static final Logger log = LoggerFactory.getLogger(SalaryRecordSearchController.class);
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "employeeNumber", "employee.employeeNumber",
            "department", "employee.department",
            "countryCode", "employee.countryCode",
            "currencyCode", "currencyCode",
            "effectiveDate", "effectiveDate",
            "recordedAt", "recordedAt");

    private final SalaryRecordService salaryRecordService;

    public SalaryRecordSearchController(SalaryRecordService salaryRecordService) {
        this.salaryRecordService = salaryRecordService;
    }

    @GetMapping
    public Page<SalaryRecordResponse> search(
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) LocalDate effectiveFrom,
            @RequestParam(required = false) LocalDate effectiveTo,
            @RequestParam(defaultValue = "false") boolean currentOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "effectiveDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        String sortField = SORTABLE_FIELDS.get(sortBy);
        if (sortField == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        log.debug("Salary record search requested: employeeNumber={}, countryCode={}, department={}, currencyCode={}, effectiveFrom={}, effectiveTo={}, currentOnly={}, page={}, size={}",
                employeeNumber, countryCode, department, currencyCode, effectiveFrom, effectiveTo, currentOnly, page, size);
        return salaryRecordService.search(employeeNumber, countryCode, department, currencyCode,
                effectiveFrom, effectiveTo, currentOnly, pageable);
    }
}

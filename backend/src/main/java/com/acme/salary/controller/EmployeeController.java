package com.acme.salary.controller;

import com.acme.salary.dto.CreateEmployeeRequest;
import com.acme.salary.dto.EmployeeResponse;
import com.acme.salary.dto.EmployeeProfileResponse;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@RestController
@RequestMapping("/api/employees")
@Validated
public class EmployeeController {
    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "employeeNumber", "firstName", "lastName", "countryCode", "department",
            "jobTitle", "jobLevel", "dateOfJoining", "status");

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public Page<EmployeeResponse> listEmployees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        log.debug("Employee directory search started: page={}, size={}, filtersApplied={}",
                page, size, q != null || countryCode != null || department != null || status != null);
        Page<EmployeeResponse> result = employeeService.search(q, countryCode, department, status, pageable);
        log.debug("Employee directory search completed: returned={}, total={}",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @GetMapping("/{id}")
    public EmployeeProfileResponse getEmployee(@PathVariable("id") String idOrNumber) {
        return employeeService.profile(idOrNumber);
    }

    @PutMapping("/{id}")
    public EmployeeProfileResponse updateEmployee(@PathVariable("id") String idOrNumber,
                                                  @Valid @RequestBody UpdateEmployeeRequest request) {
        log.debug("Update employee request started: identifier={}", idOrNumber);
        return employeeService.update(idOrNumber, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeProfileResponse createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.debug("Create employee request started: employeeNumber={}", request.employeeNumber());
        return employeeService.create(request);
    }
}

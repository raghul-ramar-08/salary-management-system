package com.acme.salary.controller;

import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.dto.SalaryRecordResponse;
import com.acme.salary.service.SalaryRecordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{employeeNumber}/salary-records")
public class SalaryRecordController {
    private static final Logger log = LoggerFactory.getLogger(SalaryRecordController.class);
    private final SalaryRecordService salaryRecordService;

    public SalaryRecordController(SalaryRecordService salaryRecordService) {
        this.salaryRecordService = salaryRecordService;
    }

    @GetMapping
    public List<SalaryRecordResponse> history(@PathVariable String employeeNumber) {
        log.debug("Salary history request started: employeeNumber={}", employeeNumber);
        return salaryRecordService.history(employeeNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaryRecordResponse addRecord(@PathVariable String employeeNumber,
                                          @Valid @RequestBody SalaryRecordRequest request) {
        log.debug("Salary change request started: employeeNumber={}, effectiveDate={}",
                employeeNumber, request.effectiveDate());
        return salaryRecordService.addRecord(employeeNumber, request);
    }
}

package com.acme.salary.dto;

import java.util.List;

public record EmployeeProfileResponse(
        EmployeeResponse employee,
        SalaryRecordResponse currentSalary,
        List<SalaryRecordResponse> salaryHistory) {
}

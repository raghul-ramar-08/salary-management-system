package com.acme.salary.dto;

import com.acme.salary.entity.Employee.EmploymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 24) String employeeNumber,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a 2-letter country code")
        String countryCode,
        @NotBlank @Size(max = 80) String department,
        @NotBlank @Size(max = 120) String jobTitle,
        @Size(max = 40) String jobLevel,
        @NotNull LocalDate dateOfJoining,
        EmploymentStatus status,
        @NotNull @Valid SalaryRecordRequest initialSalary) {
}

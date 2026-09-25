package com.acme.salary.dto;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String countryCode,
        String department,
        String jobTitle,
        String jobLevel,
        LocalDate dateOfJoining,
        EmploymentStatus status) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(), employee.getEmployeeNumber(), employee.getFirstName(),
                employee.getLastName(), employee.getCountryCode(), employee.getDepartment(),
                employee.getJobTitle(), employee.getJobLevel(), employee.getDateOfJoining(),
                employee.getStatus());
    }
}

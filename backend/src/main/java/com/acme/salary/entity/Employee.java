package com.acme.salary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "uk_employee_number", columnList = "employee_number", unique = true),
        @Index(name = "idx_employee_country_department", columnList = "country_code, department")
})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, unique = true, length = 24)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 80)
    private String department;

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;

    @Column(name = "job_level", length = 40)
    private String jobLevel;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmploymentStatus status;

    protected Employee() {
    }

    public Employee(String employeeNumber, String firstName, String lastName,
                    String countryCode, String department, String jobTitle,
                    String jobLevel, LocalDate dateOfJoining, EmploymentStatus status) {
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.countryCode = countryCode;
        this.department = department;
        this.jobTitle = jobTitle;
        this.jobLevel = jobLevel;
        this.dateOfJoining = dateOfJoining;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCountryCode() { return countryCode; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public String getJobLevel() { return jobLevel; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public EmploymentStatus getStatus() { return status; }

    // employeeNumber is a stable business identifier — not settable after creation
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public void setDepartment(String department) { this.department = department; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setJobLevel(String jobLevel) { this.jobLevel = jobLevel; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }
    public void setStatus(EmploymentStatus status) { this.status = status; }

    public enum EmploymentStatus {
        ACTIVE,
        INACTIVE
    }
}

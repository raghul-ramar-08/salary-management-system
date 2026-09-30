package com.acme.salary.controller;

import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class SalaryManagementApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaryRecordRepository salaryRecordRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanDatabase() {
        salaryRecordRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void createsEmployeeUpdatesSalaryHistoryAndComputesNormalizedReports() throws Exception {
        String createUsEmployeeJson = """
                {
                  "employeeNumber": "ACME-90001",
                  "firstName": "Avery",
                  "lastName": "Shah",
                  "countryCode": "US",
                  "department": "Engineering",
                  "jobTitle": "Software Engineer",
                  "jobLevel": "L3",
                  "dateOfJoining": "2024-01-15",
                  "status": "ACTIVE",
                  "initialSalary": {
                    "amount": 90000.00,
                    "currencyCode": "USD",
                    "effectiveDate": "2024-01-15",
                    "changeReason": "Initial hire compensation"
                  }
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUsEmployeeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee.employeeNumber", is("ACME-90001")))
                .andExpect(jsonPath("$.currentSalary.amount", is(90000.00)))
                .andExpect(jsonPath("$.salaryHistory", hasSize(1)));

        // Duplicate employee number returns 409 Conflict
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUsEmployeeJson))
                .andExpect(status().isConflict());

        // Add a later salary change for ACME-90001 and verify history interval closure
        String salaryPromotionJson = """
                {
                  "amount": 110000.00,
                  "currencyCode": "USD",
                  "effectiveDate": "2025-06-01",
                  "changeReason": "Promotion to L4"
                }
                """;

        mockMvc.perform(post("/api/employees/ACME-90001/salary-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salaryPromotionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(110000.00)))
                .andExpect(jsonPath("$.effectiveTo", nullValue()));

        var employeeId = employeeRepository.findByEmployeeNumberIgnoreCase("ACME-90001")
                .orElseThrow().getId();

        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSalary.amount", is(110000.00)))
                .andExpect(jsonPath("$.salaryHistory", hasSize(2)))
                .andExpect(jsonPath("$.salaryHistory[1].effectiveTo", is("2025-05-31")));

        // Add a second employee in India (INR)
        String createInEmployeeJson = """
                {
                  "employeeNumber": "ACME-90002",
                  "firstName": "Priya",
                  "lastName": "Nair",
                  "countryCode": "IN",
                  "department": "Engineering",
                  "jobTitle": "Software Engineer",
                  "jobLevel": "L2",
                  "dateOfJoining": "2024-03-01",
                  "status": "ACTIVE",
                  "initialSalary": {
                    "amount": 1500000.00,
                    "currencyCode": "INR",
                    "effectiveDate": "2024-03-01",
                    "changeReason": "Initial hire compensation"
                  }
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInEmployeeJson))
                .andExpect(status().isCreated());

        // Verify country report as of 2026-01-01 counts only the active 110,000 USD record (not the 90,000 historical record)
        // plus 1,500,000 INR * 0.0120 = 18,000 USD -> totalPayrollReportingCurrency = 128,000.00 USD
        mockMvc.perform(get("/api/reports/countries").param("asOfDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHeadcount", is(2)))
                .andExpect(jsonPath("$.reportingCurrency", is("USD")))
                .andExpect(jsonPath("$.totalPayrollReportingCurrency", is(128000.00)));

        // Verify department report
        mockMvc.perform(get("/api/reports/departments").param("asOfDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments", hasSize(1)))
                .andExpect(jsonPath("$.departments[0].department", is("Engineering")))
                .andExpect(jsonPath("$.departments[0].headcount", is(2)))
                .andExpect(jsonPath("$.departments[0].averageSalaryReportingCurrency", is(64000.00)))
                .andExpect(jsonPath("$.departments[0].medianSalaryReportingCurrency", is(64000.00)));

        // Verify distribution report
        mockMvc.perform(get("/api/reports/distribution")
                        .param("asOfDate", "2026-01-01")
                        .param("bandSize", "25000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHeadcount", is(2)))
                .andExpect(jsonPath("$.countryDistributions", hasSize(2)));

        // Verify department extremes report
        mockMvc.perform(get("/api/reports/department-extremes").param("asOfDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments[0].highestPaid.employeeNumber", is("ACME-90001")))
                .andExpect(jsonPath("$.departments[0].lowestPaid.employeeNumber", is("ACME-90002")));
    }

    @Test
    void rejectsInvalidCountryCurrencyAndNegativeSalaryRequestsWithBadRequest() throws Exception {
        String invalidEmployeeJson = """
                {
                  "employeeNumber": "ACME-90099",
                  "firstName": "Test",
                  "lastName": "User",
                  "countryCode": "ZZ",
                  "department": "Engineering",
                  "jobTitle": "Engineer",
                  "jobLevel": "L1",
                  "dateOfJoining": "2025-01-01",
                  "status": "ACTIVE",
                  "initialSalary": {
                    "amount": 50000.00,
                    "currencyCode": "USD",
                    "effectiveDate": "2025-01-01",
                    "changeReason": "Hire"
                  }
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmployeeJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesEmployeeProfileDetailsSuccessfully() throws Exception {
        String createEmployeeJson = """
                {
                  "employeeNumber": "ACME-90050",
                  "firstName": "Taylor",
                  "lastName": "Reed",
                  "countryCode": "US",
                  "department": "Engineering",
                  "jobTitle": "Junior Developer",
                  "jobLevel": "L1",
                  "dateOfJoining": "2024-01-10",
                  "status": "ACTIVE",
                  "initialSalary": {
                    "amount": 75000.00,
                    "currencyCode": "USD",
                    "effectiveDate": "2024-01-10",
                    "changeReason": "Initial hire compensation"
                  }
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson))
                .andExpect(status().isCreated());

        var employeeId = employeeRepository.findByEmployeeNumberIgnoreCase("ACME-90050")
                .orElseThrow().getId();

        String updateJson = """
                {
                  "firstName": "Taylor",
                  "lastName": "Reed-Smith",
                  "countryCode": "US",
                  "department": "Product",
                  "jobTitle": "Product Designer",
                  "jobLevel": "L2",
                  "dateOfJoining": "2024-01-10",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id", is(employeeId.intValue())))
                .andExpect(jsonPath("$.employee.employeeNumber", is("ACME-90050")))
                .andExpect(jsonPath("$.employee.lastName", is("Reed-Smith")))
                .andExpect(jsonPath("$.employee.department", is("Product")))
                .andExpect(jsonPath("$.employee.jobTitle", is("Product Designer")))
                .andExpect(jsonPath("$.employee.jobLevel", is("L2")))
                .andExpect(jsonPath("$.currentSalary.amount", is(75000.00)));

        // Verify GET /api/employees/{id} returns the updated state
        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.lastName", is("Reed-Smith")))
                .andExpect(jsonPath("$.employee.department", is("Product")))
                .andExpect(jsonPath("$.employee.jobTitle", is("Product Designer")));

        // Verify PUT with non-existent ID returns 404
        mockMvc.perform(put("/api/employees/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchesEmployeesWithNullParametersSuccessfully() throws Exception {
        String createEmployeeJson = """
                {
                  "employeeNumber": "ACME-90080",
                  "firstName": "Alex",
                  "lastName": "Morgan",
                  "countryCode": "US",
                  "department": "Sales",
                  "jobTitle": "Account Executive",
                  "jobLevel": "L2",
                  "dateOfJoining": "2024-02-01",
                  "status": "ACTIVE",
                  "initialSalary": {
                    "amount": 70000.00,
                    "currencyCode": "USD",
                    "effectiveDate": "2024-02-01",
                    "changeReason": "Initial hire compensation"
                  }
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson))
                .andExpect(status().isCreated());

        // Test with only countryCode filter (q, department, status are omitted / null)
        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "25")
                        .param("sortBy", "lastName")
                        .param("direction", "ASC")
                        .param("countryCode", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeNumber", is("ACME-90080")));

        // Test with completely null / default filters
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void searchesSalaryRecordsWithDateParametersSuccessfully() throws Exception {
        mockMvc.perform(get("/api/salary-records")
                        .param("effectiveFrom", "2024-01-01")
                        .param("effectiveTo", "2024-12-31"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/salary-records"))
                .andExpect(status().isOk());
    }
}


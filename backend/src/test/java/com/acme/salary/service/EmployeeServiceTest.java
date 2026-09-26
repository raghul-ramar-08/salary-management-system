package com.acme.salary.service;

import com.acme.salary.dto.CreateEmployeeRequest;
import com.acme.salary.dto.SalaryRecordRequest;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void trimsSearchFiltersAndMapsEmployeeToResponse() {
        var pageable = PageRequest.of(0, 25, Sort.by("lastName"));
        var employee = new Employee(
                "ACME-00001", "Avery", "Shah", "IN", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE);
        when(employeeRepository.search("Avery", "IN", "Engineering", EmploymentStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

        var result = employeeService.search(" Avery ", " in ", " Engineering ", EmploymentStatus.ACTIVE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("ACME-00001", result.getContent().get(0).employeeNumber());
        assertEquals("Avery", result.getContent().get(0).firstName());
        verify(employeeRepository).search("Avery", "IN", "Engineering", EmploymentStatus.ACTIVE, pageable);
    }

    @Test
    void convertsBlankFiltersToNull() {
        var pageable = PageRequest.of(0, 25);
        when(employeeRepository.search(isNull(), isNull(), isNull(), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = employeeService.search("  ", " ", null, null, pageable);

        assertEquals(0, result.getTotalElements());
        verify(employeeRepository).search(null, null, null, null, pageable);
    }

    @Test
    void profileIncludesSalaryHistoryAndSalaryActiveToday() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee("ACME-00001", "Avery", "Shah", "US", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE);
        var previous = new SalaryRecord(employee,
                new BigDecimal("80000.00"), "USD", today.minusYears(1),
                today.minusDays(1), "Previous salary");
        var current = new SalaryRecord(employee,
                new BigDecimal("90000.00"), "USD", today, null, "Current salary");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(1L))
                .thenReturn(List.of(current, previous));

        var profile = employeeService.profile(1L);

        assertEquals("ACME-00001", profile.employee().employeeNumber());
        assertEquals(new BigDecimal("90000.00"), profile.currentSalary().amount());
        assertEquals(2, profile.salaryHistory().size());
        assertEquals("Current salary", profile.salaryHistory().get(0).changeReason());
    }

    @Test
    void profileCanBeLookedUpByEmployeeNumber() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee("ACME-00001", "Avery", "Shah", "US", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE);
        var current = new SalaryRecord(employee,
                new BigDecimal("90000.00"), "USD", today, null, "Current salary");
        when(employeeRepository.findByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployee_EmployeeNumberIgnoreCaseOrderByEffectiveDateDescRecordedAtDesc("ACME-00001"))
                .thenReturn(List.of(current));

        var profile = employeeService.profile("ACME-00001");

        assertEquals("ACME-00001", profile.employee().employeeNumber());
        assertEquals(new BigDecimal("90000.00"), profile.currentSalary().amount());
        assertEquals(1, profile.salaryHistory().size());
    }

    @Test
    void profileHasNoCurrentSalaryWhenAllRecordsAreFutureDated() {
        Employee employee = new Employee("ACME-00002", "Jordan", "Patel", "IN", "Finance",
                "Analyst", "L1", LocalDate.of(2024, 1, 1), EmploymentStatus.ACTIVE);
        var future = new SalaryRecord(employee,
                new BigDecimal("700000.00"), "INR", LocalDate.now().plusDays(1),
                null, "Future salary");
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(2L))
                .thenReturn(List.of(future));

        var profile = employeeService.profile(2L);

        assertNull(profile.currentSalary());
        assertEquals(1, profile.salaryHistory().size());
    }

    @Test
    void profileHasNoCurrentSalaryWhenAllRecordsAreExpired() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee("ACME-00003", "Morgan", "Kim", "GB", "Sales",
                "Specialist", "L2", LocalDate.of(2020, 3, 10), EmploymentStatus.INACTIVE);
        var expired = new SalaryRecord(employee,
                new BigDecimal("48000.00"), "GBP", today.minusYears(2),
                today.minusMonths(1), "Ended employment period");
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(3L))
                .thenReturn(List.of(expired));

        var profile = employeeService.profile(3L);

        assertNull(profile.currentSalary());
        assertEquals(1, profile.salaryHistory().size());
    }

    @Test
    void profileReturnsNotFoundForUnknownEmployee() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> employeeService.profile(999L));

        assertEquals(NOT_FOUND, error.getStatusCode());
    }

    @Test
    void createSavesEmployeeAndInitialSalaryTransactionally() {
        LocalDate today = LocalDate.now();
        var request = new CreateEmployeeRequest(
                " acme-10001 ",
                " Priya ",
                " Nair ",
                " in ",
                " Engineering ",
                " Senior Software Engineer ",
                " L4 ",
                today.minusDays(5),
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        new BigDecimal("1850000.00"),
                        " inr ",
                        today,
                        " Initial hire compensation "));

        when(employeeRepository.existsByEmployeeNumberIgnoreCase("ACME-10001")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(salaryRecordRepository.save(any(SalaryRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = employeeService.create(request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertEquals("ACME-10001", employeeCaptor.getValue().getEmployeeNumber());
        assertEquals("IN", employeeCaptor.getValue().getCountryCode());
        assertEquals("Priya", employeeCaptor.getValue().getFirstName());

        ArgumentCaptor<SalaryRecord> salaryCaptor = ArgumentCaptor.forClass(SalaryRecord.class);
        verify(salaryRecordRepository).save(salaryCaptor.capture());
        assertEquals("INR", salaryCaptor.getValue().getCurrencyCode());
        assertEquals(new BigDecimal("1850000.00"), salaryCaptor.getValue().getAmount());
        assertNull(salaryCaptor.getValue().getEffectiveTo());

        assertNotNull(created.currentSalary());
        assertEquals(1, created.salaryHistory().size());
    }

    @Test
    void createRejectsDuplicateEmployeeNumberWithConflict() {
        LocalDate today = LocalDate.now();
        var request = new CreateEmployeeRequest(
                "acme-00001",
                "Avery",
                "Shah",
                "US",
                "Engineering",
                "Software Engineer",
                "L2",
                today,
                EmploymentStatus.ACTIVE,
                new SalaryRecordRequest(
                        new BigDecimal("95000.00"),
                        "USD",
                        today,
                        "Initial hire"));

        when(employeeRepository.existsByEmployeeNumberIgnoreCase("ACME-00001")).thenReturn(true);

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> employeeService.create(request));

        assertEquals(CONFLICT, error.getStatusCode());
        verify(employeeRepository, never()).save(any(Employee.class));
        verify(salaryRecordRepository, never()).save(any(SalaryRecord.class));
    }

    @Test
    void updateAppliesAllMutableFieldsAndReturnsRefreshedProfile() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee("ACME-00001", "Avery", "Shah", "IN", "Engineering",
                "Software Engineer", "L2", LocalDate.of(2022, 4, 1), EmploymentStatus.ACTIVE);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        // buildProfile fetches salary history for the saved employee — stub with any() to avoid strict-stub failure
        org.mockito.Mockito.lenient()
                .when(salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(any()))
                .thenReturn(List.of());

        var request = new UpdateEmployeeRequest(
                " Riley ", " Chen ", "US", "Product", "Staff Engineer", "L5",
                today, EmploymentStatus.ACTIVE);

        var result = employeeService.update("1", request);

        assertEquals("Riley", result.employee().firstName());
        assertEquals("Chen", result.employee().lastName());
        assertEquals("US", result.employee().countryCode());
        assertEquals("Product", result.employee().department());
        assertEquals("Staff Engineer", result.employee().jobTitle());
        assertEquals("L5", result.employee().jobLevel());
        // employeeNumber must not change
        assertEquals("ACME-00001", result.employee().employeeNumber());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void updateLooksUpByEmployeeNumber() {
        LocalDate today = LocalDate.now();
        Employee employee = new Employee("ACME-00001", "Avery", "Shah", "IN", "Engineering",
                "Software Engineer", "L2", today, EmploymentStatus.ACTIVE);

        when(employeeRepository.findByEmployeeNumberIgnoreCase("ACME-00001"))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(salaryRecordRepository.findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(any()))
                .thenReturn(List.of());

        var request = new UpdateEmployeeRequest(
                "Avery", "Shah", "IN", "Finance", "Senior Analyst", null,
                today, EmploymentStatus.INACTIVE);

        var result = employeeService.update("ACME-00001", request);

        assertEquals("Finance", result.employee().department());
        assertEquals(EmploymentStatus.INACTIVE, result.employee().status());
    }

    @Test
    void updateReturnsNotFoundForUnknownEmployee() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> employeeService.update("999",
                        new UpdateEmployeeRequest("A", "B", "US", "Engineering",
                                "Engineer", null, LocalDate.now(), EmploymentStatus.ACTIVE)));

        assertEquals(NOT_FOUND, error.getStatusCode());
        verify(employeeRepository, never()).save(any(Employee.class));
    }
}

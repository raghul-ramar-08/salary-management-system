package com.acme.salary.seed;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import com.acme.salary.repository.EmployeeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
public class EmployeeDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EmployeeDataSeeder.class);
    private static final int EMPLOYEE_COUNT = 10_000;
    private static final int BATCH_SIZE = 500;

    private static final List<String> FIRST_NAMES = List.of(
            "Avery", "Jordan", "Morgan", "Taylor", "Casey", "Riley", "Jamie", "Alex",
            "Sam", "Cameron", "Drew", "Robin", "Kai", "Sasha", "Devon", "Quinn");
    private static final List<String> LAST_NAMES = List.of(
            "Shah", "Patel", "Kim", "Garcia", "Wilson", "Chen", "Kumar", "Brown",
            "Singh", "Miller", "Lopez", "Davis", "Gupta", "Martin", "Clark", "Roy");
    private static final List<String> COUNTRIES = List.of("US", "IN", "GB", "DE", "SG");
    private static final List<String> DEPARTMENTS = List.of(
            "Engineering", "Finance", "Sales", "People", "Operations", "Product");
    private static final List<String> JOB_TITLES = List.of(
            "Analyst", "Specialist", "Software Engineer", "Product Manager", "Team Lead", "Director");
    private static final List<String> JOB_LEVELS = List.of("L1", "L2", "L3", "L4", "L5", "L6");

    private final EmployeeRepository employeeRepository;

    public EmployeeDataSeeder(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (employeeRepository.count() > 0) {
            log.info("Employee seed skipped: existing employees found");
            return;
        }

        log.info("Employee seed started: targetCount={}", EMPLOYEE_COUNT);
        for (int start = 0; start < EMPLOYEE_COUNT; start += BATCH_SIZE) {
            List<Employee> batch = new ArrayList<>(BATCH_SIZE);
            int end = Math.min(start + BATCH_SIZE, EMPLOYEE_COUNT);
            for (int index = start; index < end; index++) {
                batch.add(createEmployee(index));
            }
            employeeRepository.saveAll(batch);
            log.debug("Employee seed batch saved: count={}", end - start);
        }
        log.info("Employee seed completed: count={}", EMPLOYEE_COUNT);
    }

    private Employee createEmployee(int index) {
        return new Employee(
                "ACME-%05d".formatted(index + 1),
                FIRST_NAMES.get(index % FIRST_NAMES.size()),
                LAST_NAMES.get((index / FIRST_NAMES.size()) % LAST_NAMES.size()),
                COUNTRIES.get(index % COUNTRIES.size()),
                DEPARTMENTS.get((index / 5) % DEPARTMENTS.size()),
                JOB_TITLES.get((index / 11) % JOB_TITLES.size()),
                JOB_LEVELS.get((index / 19) % JOB_LEVELS.size()),
                LocalDate.of(2010 + index % 15, 1 + index % 12, 1 + index % 28),
                index % 20 == 0 ? EmploymentStatus.INACTIVE : EmploymentStatus.ACTIVE);
    }
}

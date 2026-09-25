package com.acme.salary.seed;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.SalaryRecord;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@Order(2)
public class SalaryRecordDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SalaryRecordDataSeeder.class);
    private static final int EMPLOYEE_PAGE_SIZE = 500;
    private static final int RECORDS_PER_EMPLOYEE = 2;
    private static final Map<String, String> CURRENCIES_BY_COUNTRY = Map.of(
            "IN", "INR", "US", "USD", "GB", "GBP", "DE", "EUR", "SG", "SGD");
    private static final Map<String, BigDecimal> ANNUAL_BASE_BY_COUNTRY = Map.of(
            "IN", new BigDecimal("600000"),
            "US", new BigDecimal("70000"),
            "GB", new BigDecimal("45000"),
            "DE", new BigDecimal("50000"),
            "SG", new BigDecimal("60000"));

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public SalaryRecordDataSeeder(EmployeeRepository employeeRepository,
                                  SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (salaryRecordRepository.count() > 0) {
            log.info("Salary seed skipped: existing salary records found");
            return;
        }

        long employeeCount = employeeRepository.count();
        if (employeeCount == 0) {
            log.warn("Salary seed skipped: no employees found");
            return;
        }

        log.info("Salary seed started: employees={}, recordsPerEmployee={}",
                employeeCount, RECORDS_PER_EMPLOYEE);
        int pageNumber = 0;
        long seededCount = 0;
        Page<Employee> employeePage;
        do {
            employeePage = employeeRepository.findAll(PageRequest.of(pageNumber++, EMPLOYEE_PAGE_SIZE,
                    Sort.by(Sort.Direction.ASC, "id")));
            List<SalaryRecord> batch = new ArrayList<>(employeePage.getNumberOfElements() * RECORDS_PER_EMPLOYEE);
            for (Employee employee : employeePage.getContent()) {
                batch.addAll(createHistory(employee));
            }
            salaryRecordRepository.saveAll(batch);
            seededCount += batch.size();
            log.debug("Salary seed batch saved: batchSize={}, totalSaved={}", batch.size(), seededCount);
        } while (employeePage.hasNext());

        log.info("Salary seed completed: records={}", seededCount);
    }

    private List<SalaryRecord> createHistory(Employee employee) {
        String currencyCode = CURRENCIES_BY_COUNTRY.get(employee.getCountryCode());
        if (currencyCode == null) {
            throw new IllegalStateException("No seed currency configured for country " + employee.getCountryCode());
        }

        BigDecimal initialAmount = annualSalary(employee);
        LocalDate firstEffectiveDate = employee.getDateOfJoining().plusMonths(1);
        LocalDate reviewEffectiveDate = firstEffectiveDate.plusYears(1);
        BigDecimal reviewIncrease = new BigDecimal("1.05")
                .add(new BigDecimal("0.01").multiply(BigDecimal.valueOf(employee.getId() % 4)));
        BigDecimal reviewedAmount = initialAmount.multiply(reviewIncrease).setScale(2, RoundingMode.HALF_UP);

        return List.of(
                new SalaryRecord(employee, initialAmount, currencyCode, firstEffectiveDate,
                        reviewEffectiveDate.minusDays(1), "Initial salary entry"),
                new SalaryRecord(employee, reviewedAmount, currencyCode, reviewEffectiveDate,
                        "Annual compensation review"));
    }

    private BigDecimal annualSalary(Employee employee) {
        BigDecimal countryBase = ANNUAL_BASE_BY_COUNTRY.get(employee.getCountryCode());
        int level = employee.getJobLevel() == null
                ? 1 : Integer.parseInt(employee.getJobLevel().substring(1));
        BigDecimal levelFactor = new BigDecimal("0.85")
                .add(new BigDecimal("0.10").multiply(BigDecimal.valueOf(level)));
        BigDecimal departmentFactor = switch (employee.getDepartment()) {
            case "Engineering", "Product" -> new BigDecimal("1.10");
            default -> BigDecimal.ONE;
        };
        BigDecimal employeeVariance = new BigDecimal("0.95")
                .add(new BigDecimal("0.015").multiply(BigDecimal.valueOf((employee.getId() - 1) % 7)));

        return countryBase.multiply(levelFactor).multiply(departmentFactor)
                .multiply(employeeVariance).setScale(2, RoundingMode.HALF_UP);
    }
}

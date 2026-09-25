package com.acme.salary.repository;

import com.acme.salary.entity.SalaryRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    @EntityGraph(attributePaths = "employee")
    @Query("""
            select sr from SalaryRecord sr
            join sr.employee e
            where (:employeeNumber is null
                    or lower(e.employeeNumber) like lower(concat('%', :employeeNumber, '%')))
              and (:countryCode is null or e.countryCode = :countryCode)
              and (:department is null or lower(e.department) = lower(:department))
              and (:currencyCode is null or sr.currencyCode = :currencyCode)
              and (:effectiveFrom is null or sr.effectiveDate >= :effectiveFrom)
              and (:effectiveTo is null or sr.effectiveDate <= :effectiveTo)
              and (:currentOnly = false or (
                    sr.effectiveDate <= :asOfDate
                    and (sr.effectiveTo is null or sr.effectiveTo >= :asOfDate)
              ))
            """)
    Page<SalaryRecord> search(
            @Param("employeeNumber") String employeeNumber,
            @Param("countryCode") String countryCode,
            @Param("department") String department,
            @Param("currencyCode") String currencyCode,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("currentOnly") boolean currentOnly,
            @Param("asOfDate") LocalDate asOfDate,
            Pageable pageable);

    List<SalaryRecord> findByEmployee_EmployeeNumberIgnoreCaseOrderByEffectiveDateDescRecordedAtDesc(
            String employeeNumber);

    List<SalaryRecord> findByEmployee_IdOrderByEffectiveDateDescRecordedAtDesc(Long employeeId);

    List<SalaryRecord> findByEmployee_IdOrderByEffectiveDateAsc(Long employeeId);

    boolean existsByEmployee_IdAndEffectiveDate(Long employeeId, LocalDate effectiveDate);

    @EntityGraph(attributePaths = "employee")
    @Query("""
            select sr from SalaryRecord sr
            join sr.employee e
            where sr.effectiveDate <= :asOfDate
              and (sr.effectiveTo is null or sr.effectiveTo >= :asOfDate)
              and (:includeInactive = true or e.status = com.acme.salary.entity.Employee$EmploymentStatus.ACTIVE)
            """)
    List<SalaryRecord> findActiveRecordsAsOf(
            @Param("asOfDate") LocalDate asOfDate,
            @Param("includeInactive") boolean includeInactive);
}

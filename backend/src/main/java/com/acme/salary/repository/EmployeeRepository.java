package com.acme.salary.repository;

import com.acme.salary.entity.Employee;
import com.acme.salary.entity.Employee.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNumberIgnoreCase(String employeeNumber);

    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);

    @Query("""
            select e from Employee e
            where (cast(:q as string) is null
                   or lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', cast(:q as string), '%'))
                   or lower(e.employeeNumber) like lower(concat('%', cast(:q as string), '%')))
              and (cast(:countryCode as string) is null or e.countryCode = cast(:countryCode as string))
              and (cast(:department as string) is null or lower(e.department) = lower(cast(:department as string)))
              and (:status is null or e.status = :status)
            """)
    Page<Employee> search(
            @Param("q") String query,
            @Param("countryCode") String countryCode,
            @Param("department") String department,
            @Param("status") EmploymentStatus status,
            Pageable pageable);
}

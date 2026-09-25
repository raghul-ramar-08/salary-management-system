package com.acme.salary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "salary_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_salary_employee_effective_date",
                columnNames = {"employee_id", "effective_date"}),
        indexes = @Index(name = "idx_salary_employee_effective_date",
                columnList = "employee_id, effective_date"))
public class SalaryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "change_reason", nullable = false, length = 240)
    private String changeReason;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected SalaryRecord() { }

    public SalaryRecord(Employee employee, BigDecimal amount, String currencyCode,
                        LocalDate effectiveDate, String changeReason) {
        this(employee, amount, currencyCode, effectiveDate, null, changeReason);
    }

    public SalaryRecord(Employee employee, BigDecimal amount, String currencyCode,
                        LocalDate effectiveDate, LocalDate effectiveTo, String changeReason) {
        this.employee = employee;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.effectiveDate = effectiveDate;
        this.effectiveTo = effectiveTo;
        this.changeReason = changeReason;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getChangeReason() { return changeReason; }
    public Instant getRecordedAt() { return recordedAt; }
}

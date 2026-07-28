package com.financas.account.domain;

import com.financas.supplier.domain.Supplier;
import com.financas.unit.domain.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fund fund;

    @Column(nullable = false)
    private boolean recurring;

    @ManyToOne(optional = true)
    @JoinColumn(name = "unit_id", nullable = true)
    private Unit unit;

    @ManyToOne(optional = true)
    @JoinColumn(name = "supplier_id", nullable = true)
    private Supplier supplier;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    protected Account() {
    }

    public Account(
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Fund fund,
            boolean recurring,
            Unit unit,
            Supplier supplier,
            LocalDate paymentDate,
            String observations) {
        this.type = type;
        this.amount = amount;
        this.dueDate = dueDate;
        this.description = description;
        this.fund = fund;
        this.recurring = recurring;
        this.unit = unit;
        this.supplier = supplier;
        this.paymentDate = paymentDate;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Fund getFund() {
        return fund;
    }

    public void setFund(Fund fund) {
        this.fund = fund;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public boolean isPaid() {
        return paymentDate != null;
    }
}

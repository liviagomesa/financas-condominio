package com.financas.receivable.domain;

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
@Table(name = "receivable")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_account", nullable = false)
    private TargetAccount targetAccount;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    protected Receivable() {
    }

    public Receivable(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring,
            Unit unit) {
        this(amount, dueDate, description, targetAccount, recurring, unit, null);
    }

    public Receivable(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring,
            Unit unit,
            LocalDate paymentDate) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.description = description;
        this.targetAccount = targetAccount;
        this.recurring = recurring;
        this.unit = unit;
        this.paymentDate = paymentDate;
    }

    public Long getId() {
        return id;
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

    public TargetAccount getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(TargetAccount targetAccount) {
        this.targetAccount = targetAccount;
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public boolean isPaid() {
        return paymentDate != null;
    }
}

package com.financas.recurringcharge.domain;

import com.financas.account.domain.AccountType;
import com.financas.fund.domain.Fund;
import com.financas.party.domain.Party;
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
@Table(name = "recurring_charge")
public class RecurringCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_day", nullable = false)
    private Integer dueDay;

    @Column(nullable = false)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @ManyToOne(optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "deactivated_at")
    private LocalDate deactivatedAt;

    @Column(name = "last_generation_failed", nullable = false)
    private boolean lastGenerationFailed;

    protected RecurringCharge() {
    }

    public RecurringCharge(
            AccountType type,
            BigDecimal amount,
            Integer dueDay,
            String description,
            Fund fund,
            Party party,
            String observations) {
        this.type = type;
        this.amount = amount;
        this.dueDay = dueDay;
        this.description = description;
        this.fund = fund;
        this.party = party;
        this.observations = observations;
        this.lastGenerationFailed = false;
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

    public Integer getDueDay() {
        return dueDay;
    }

    public String getDescription() {
        return description;
    }

    public Fund getFund() {
        return fund;
    }

    public Party getParty() {
        return party;
    }

    public String getObservations() {
        return observations;
    }

    public LocalDate getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(LocalDate deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public boolean isLastGenerationFailed() {
        return lastGenerationFailed;
    }

    public void setLastGenerationFailed(boolean lastGenerationFailed) {
        this.lastGenerationFailed = lastGenerationFailed;
    }

    public boolean isActive() {
        return deactivatedAt == null;
    }
}

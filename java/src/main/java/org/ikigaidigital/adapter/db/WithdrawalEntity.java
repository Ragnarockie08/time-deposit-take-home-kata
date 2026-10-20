package org.ikigaidigital.adapter.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "withdrawals")
class WithdrawalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "time_deposit_id", nullable = false)
    private Integer timeDepositId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    protected WithdrawalEntity() {
    }

    WithdrawalEntity(Integer timeDepositId, BigDecimal amount, LocalDate date) {
        this.timeDepositId = timeDepositId;
        this.amount = amount;
        this.date = date;
    }

    Integer getId() {
        return id;
    }

    Integer getTimeDepositId() {
        return timeDepositId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    LocalDate getDate() {
        return date;
    }
}

package org.ikigaidigital.adapter.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "time_deposits")
class TimeDepositEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected TimeDepositEntity() {
    }

    TimeDepositEntity(String planType, int days, BigDecimal balance) {
        this.planType = planType;
        this.days = days;
        this.balance = balance;
    }

    Integer getId() {
        return id;
    }

    String getPlanType() {
        return planType;
    }

    int getDays() {
        return days;
    }

    BigDecimal getBalance() {
        return balance;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}

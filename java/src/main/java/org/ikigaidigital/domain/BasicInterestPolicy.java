package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;

/** Basic plan: 1% a year, paid monthly, no further conditions. */
public final class BasicInterestPolicy implements InterestPolicy {

    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.01");

    @Override
    public BigDecimal monthlyInterest(TimeDeposit deposit) {
        return InterestPolicy.monthlyInterestAt(ANNUAL_RATE, deposit);
    }
}

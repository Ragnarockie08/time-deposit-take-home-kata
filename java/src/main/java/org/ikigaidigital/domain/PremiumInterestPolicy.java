package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;

/** Premium plan: 5% a year, paid monthly, interest starts after 45 days. */
public final class PremiumInterestPolicy implements InterestPolicy {

    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.05");
    private static final int DAYS_BEFORE_INTEREST = 45;

    @Override
    public BigDecimal monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= DAYS_BEFORE_INTEREST) {
            return BigDecimal.ZERO;
        }
        return InterestPolicy.monthlyInterestAt(ANNUAL_RATE, deposit);
    }
}

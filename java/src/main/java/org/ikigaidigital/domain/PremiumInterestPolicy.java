package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

/** Premium plan: 5% a year, paid monthly, interest starts after 45 days. */
public final class PremiumInterestPolicy implements InterestPolicy {

    private static final double ANNUAL_RATE = 0.05;
    private static final int DAYS_BEFORE_INTEREST = 45;

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= DAYS_BEFORE_INTEREST) {
            return 0;
        }
        return deposit.getBalance() * ANNUAL_RATE / 12;
    }
}

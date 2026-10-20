package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

/** Student plan: 3% a year, paid monthly, no interest after one year. */
public final class StudentInterestPolicy implements InterestPolicy {

    private static final double ANNUAL_RATE = 0.03;
    // "No interest after 1 year": the original code pays while days < 366, so day 365 is the last paid day.
    private static final int LAST_INTEREST_DAY = 365;

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > LAST_INTEREST_DAY) {
            return 0;
        }
        return deposit.getBalance() * ANNUAL_RATE / 12;
    }
}

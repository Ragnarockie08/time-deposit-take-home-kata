package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * The interest rule of one plan (Strategy). Returns the unrounded interest for one month;
 * rounding and the grace period shared by all plans are applied by the calculator.
 */
@FunctionalInterface
public interface InterestPolicy {

    InterestPolicy NO_INTEREST = deposit -> BigDecimal.ZERO;

    BigDecimal monthlyInterest(TimeDeposit deposit);

    static BigDecimal monthlyInterestAt(BigDecimal annualRate, TimeDeposit deposit) {
        return BigDecimal.valueOf(deposit.getBalance())
                .multiply(annualRate)
                .divide(BigDecimal.valueOf(12), MathContext.DECIMAL128);
    }
}

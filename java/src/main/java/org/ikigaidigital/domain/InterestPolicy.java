package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

/**
 * The interest rule of one plan (Strategy). Returns the unrounded interest for one month;
 * rounding and the grace period shared by all plans are applied by the calculator.
 */
@FunctionalInterface
public interface InterestPolicy {

    InterestPolicy NO_INTEREST = deposit -> 0;

    double monthlyInterest(TimeDeposit deposit);
}

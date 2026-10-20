package org.ikigaidigital;

import org.ikigaidigital.domain.BasicInterestPolicy;
import org.ikigaidigital.domain.InterestPolicy;
import org.ikigaidigital.domain.PlanType;
import org.ikigaidigital.domain.PremiumInterestPolicy;
import org.ikigaidigital.domain.StudentInterestPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Applies one month of interest to each deposit. The interest rule of each plan is an {@link InterestPolicy}
 * looked up by plan type; a new plan needs a new policy and a registry entry, not a change here.
 */
public class TimeDepositCalculator {

    // Applies to every existing plan, so it stays here rather than in each policy.
    private static final int GRACE_PERIOD_DAYS = 30;

    private final Map<PlanType, InterestPolicy> policies;

    public TimeDepositCalculator() {
        this(defaultPolicies());
    }

    public TimeDepositCalculator(Map<PlanType, InterestPolicy> policies) {
        Map<PlanType, InterestPolicy> copy = new EnumMap<>(PlanType.class);
        copy.putAll(policies);
        this.policies = Collections.unmodifiableMap(copy);
    }

    public static Map<PlanType, InterestPolicy> defaultPolicies() {
        Map<PlanType, InterestPolicy> policies = new EnumMap<>(PlanType.class);
        policies.put(PlanType.BASIC, new BasicInterestPolicy());
        policies.put(PlanType.STUDENT, new StudentInterestPolicy());
        policies.put(PlanType.PREMIUM, new PremiumInterestPolicy());
        return policies;
    }

    public void updateBalance(List<TimeDeposit> xs) {
        for (TimeDeposit deposit : xs) {
            double interest = monthlyInterest(deposit);
            double roundedInterest = new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }

    private double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= GRACE_PERIOD_DAYS) {
            return 0;
        }
        return policyFor(deposit).monthlyInterest(deposit);
    }

    // Unknown plan types earn no interest, as in the original implementation.
    private InterestPolicy policyFor(TimeDeposit deposit) {
        return PlanType.fromCode(deposit.getPlanType())
                .map(policies::get)
                .orElse(InterestPolicy.NO_INTEREST);
    }
}

package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InterestPolicyTest {

    @Test
    void basicPaysOnePercentAYearMonthly() {
        assertThat(new BasicInterestPolicy().monthlyInterest(deposit("basic", 1200.00, 31))).isEqualTo(1.0);
    }

    @ParameterizedTest(name = "{0} days -> {1}")
    @CsvSource({"365, 3.0", "366, 0.0"})
    void studentStopsPayingAfterOneYear(int days, double expected) {
        assertThat(new StudentInterestPolicy().monthlyInterest(deposit("student", 1200.00, days))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} days -> {1}")
    @CsvSource({"45, 0.0", "46, 5.0"})
    void premiumStartsPayingAfterFortyFiveDays(int days, double expected) {
        assertThat(new PremiumInterestPolicy().monthlyInterest(deposit("premium", 1200.00, days))).isEqualTo(expected);
    }

    @Test
    void everyPlanTypeHasADefaultPolicy() {
        assertThat(TimeDepositCalculator.defaultPolicies()).containsOnlyKeys(PlanType.values());
    }

    @Test
    void calculatorUsesTheRegisteredPolicyForAPlan() {
        TimeDepositCalculator calculator = new TimeDepositCalculator(Map.of(PlanType.BASIC, deposit -> 10.0));
        TimeDeposit deposit = deposit("basic", 100.00, 31);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(110.00);
    }

    @Test
    void planTypeCodesMatchTheStoredValues() {
        assertThat(PlanType.fromCode("premium")).contains(PlanType.PREMIUM);
        assertThat(PlanType.fromCode("Premium")).isEmpty();
    }

    private static TimeDeposit deposit(String planType, double balance, int days) {
        return new TimeDeposit(1, planType, balance, days);
    }
}

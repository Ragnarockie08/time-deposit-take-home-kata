package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvSource({"basic", "student", "premium"})
    void noPlanEarnsInterestDuringTheFirstThirtyDays(String planType) {
        assertThat(balanceAfterOneUpdate(planType, 1000.00, 30)).isEqualTo(1000.00);
    }

    @ParameterizedTest(name = "{0} days")
    @CsvSource({"31", "1000"})
    void basicEarnsOnePercentAYearFromDayThirtyOne(int days) {
        assertThat(balanceAfterOneUpdate("basic", 1000.00, days)).isEqualTo(1000.83);
    }

    @ParameterizedTest(name = "{0} days")
    @CsvSource({"31", "365"})
    void studentEarnsThreePercentAYearUntilDayThreeHundredSixtyFive(int days) {
        assertThat(balanceAfterOneUpdate("student", 1000.00, days)).isEqualTo(1002.50);
    }

    @Test
    void studentEarnsNothingFromDayThreeHundredSixtySix() {
        assertThat(balanceAfterOneUpdate("student", 1000.00, 366)).isEqualTo(1000.00);
    }

    @Test
    void premiumEarnsNothingUntilDayFortyFive() {
        assertThat(balanceAfterOneUpdate("premium", 1000.00, 45)).isEqualTo(1000.00);
    }

    @ParameterizedTest(name = "{0} days")
    @CsvSource({"46", "1000"})
    void premiumEarnsFivePercentAYearFromDayFortySix(int days) {
        assertThat(balanceAfterOneUpdate("premium", 1000.00, days)).isEqualTo(1004.17);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @CsvSource({"gold", "Basic"})
    void unknownOrDifferentlyCasedPlanTypesEarnNothing(String planType) {
        assertThat(balanceAfterOneUpdate(planType, 1000.00, 100)).isEqualTo(1000.00);
    }

    @Test
    void keepsTheResultOfTheOriginalStarterExample() {
        assertThat(balanceAfterOneUpdate("basic", 1234567.00, 45)).isEqualTo(1235595.81);
    }

    @ParameterizedTest(name = "{0}, balance {1}, {2} days -> {3}")
    @CsvSource({
            "basic,   18.00, 31, 18.02",
            "student, 6.00,  31, 6.02",
            "premium, 18.00, 46, 18.08",
    })
    void roundsExactHalfCentInterestUp(
            String planType, double balance, int days, double expected) {
        assertThat(balanceAfterOneUpdate(planType, balance, days)).isEqualTo(expected);
    }

    @Test
    void keepsTheBalanceInWholeCents() {
        assertThat(balanceAfterOneUpdate("premium", 1.37, 46)).isEqualTo(1.38);
    }

    @Test
    void updatesEveryDepositInTheListIndependently() {
        TimeDeposit basic = new TimeDeposit(1, "basic", 1000.00, 31);
        TimeDeposit student = new TimeDeposit(2, "student", 1000.00, 366);
        TimeDeposit premium = new TimeDeposit(3, "premium", 1000.00, 46);

        calculator.updateBalance(List.of(basic, student, premium));

        assertThat(basic.getBalance()).isEqualTo(1000.83);
        assertThat(student.getBalance()).isEqualTo(1000.00);
        assertThat(premium.getBalance()).isEqualTo(1004.17);
    }

    @Test
    void appliesInterestToTheGrownBalanceOnEveryCall() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1000.00, 31);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1001.66);
    }

    @Test
    void acceptsAnEmptyList() {
        assertThatCode(() -> calculator.updateBalance(new ArrayList<>())).doesNotThrowAnyException();
    }

    private double balanceAfterOneUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}

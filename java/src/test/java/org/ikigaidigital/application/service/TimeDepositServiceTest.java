package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.InterestRunInProgressException;
import org.ikigaidigital.application.port.in.TimeDepositWithWithdrawals;
import org.ikigaidigital.application.port.out.InterestRunLock;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeDepositServiceTest {

    private final FakeTimeDepositRepository deposits = new FakeTimeDepositRepository();
    private final FakeWithdrawalRepository withdrawals = new FakeWithdrawalRepository();
    private final FakeLock lock = new FakeLock();
    private final TimeDepositService service =
            new TimeDepositService(deposits, withdrawals, lock, new TimeDepositCalculator());

    @Test
    void updateAppliesOneMonthOfInterestAndSavesTheNewBalances() {
        deposits.stored.add(new TimeDeposit(1, "premium", 1200.00, 60));
        deposits.stored.add(new TimeDeposit(2, "basic", 1200.00, 10));

        service.updateAllBalances();

        assertThat(deposits.savedBalances).containsExactly(1205.0, 1200.0);
        assertThat(deposits.loadedForUpdate).isTrue();
    }

    @Test
    void updateIsRejectedWhenAnotherRunHoldsTheLock() {
        lock.available = false;
        deposits.stored.add(new TimeDeposit(1, "premium", 1200.00, 60));

        assertThatThrownBy(service::updateAllBalances).isInstanceOf(InterestRunInProgressException.class);

        assertThat(deposits.loadedForUpdate).isFalse();
        assertThat(deposits.savedBalances).isNull();
    }

    @Test
    void getGroupsWithdrawalsUnderTheirDepositAndKeepsOrder() {
        deposits.stored.add(new TimeDeposit(3, "basic", 100.00, 40));
        deposits.stored.add(new TimeDeposit(1, "student", 200.00, 50));
        deposits.stored.add(new TimeDeposit(2, "premium", 300.00, 60));
        Withdrawal a = new Withdrawal(10, 1, new BigDecimal("5.00"), LocalDate.of(2026, 1, 2));
        Withdrawal b = new Withdrawal(11, 3, new BigDecimal("6.00"), LocalDate.of(2026, 2, 3));
        Withdrawal c = new Withdrawal(12, 1, new BigDecimal("7.00"), LocalDate.of(2026, 3, 4));
        withdrawals.stored.addAll(List.of(a, b, c));

        List<TimeDepositWithWithdrawals> result = service.getAllTimeDeposits();

        assertThat(result).extracting(r -> r.deposit().getId()).containsExactly(3, 1, 2);
        assertThat(result.get(0).withdrawals()).containsExactly(b);
        assertThat(result.get(1).withdrawals()).containsExactly(a, c);
        assertThat(result.get(2).withdrawals()).isEmpty();
    }

    @Test
    void getReturnsAnEmptyListWhenThereAreNoDeposits() {
        assertThat(service.getAllTimeDeposits()).isEmpty();
    }

    private static class FakeTimeDepositRepository implements TimeDepositRepository {
        final List<TimeDeposit> stored = new ArrayList<>();
        boolean loadedForUpdate;
        List<Double> savedBalances;

        @Override
        public List<TimeDeposit> findAll() {
            return List.copyOf(stored);
        }

        @Override
        public List<TimeDeposit> findAllForUpdate() {
            loadedForUpdate = true;
            return List.copyOf(stored);
        }

        @Override
        public void saveBalances(List<TimeDeposit> deposits) {
            savedBalances = deposits.stream().map(TimeDeposit::getBalance).toList();
        }
    }

    private static class FakeWithdrawalRepository implements WithdrawalRepository {
        final List<Withdrawal> stored = new ArrayList<>();

        @Override
        public List<Withdrawal> findAll() {
            return List.copyOf(stored);
        }
    }

    private static class FakeLock implements InterestRunLock {
        boolean available = true;

        @Override
        public boolean tryAcquire() {
            return available;
        }
    }
}

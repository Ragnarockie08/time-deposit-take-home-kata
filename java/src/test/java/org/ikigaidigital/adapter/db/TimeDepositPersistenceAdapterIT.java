package org.ikigaidigital.adapter.db;

import jakarta.persistence.EntityManager;
import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class,
        WithdrawalPersistenceAdapter.class, PostgresInterestRunLock.class})
class TimeDepositPersistenceAdapterIT {

    @Autowired
    private TimeDepositPersistenceAdapter deposits;
    @Autowired
    private WithdrawalPersistenceAdapter withdrawals;
    @Autowired
    private PostgresInterestRunLock lock;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanTables() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            jdbc.update("delete from withdrawals");
            jdbc.update("delete from time_deposits");
        }
    }

    private void insertDeposit(int id, String plan, int days, String balance) {
        jdbc.update("insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)",
                id, plan, days, new BigDecimal(balance));
    }

    @Test
    void findAllMapsFieldsAndOrdersById() {
        insertDeposit(3, "premium", 50, "300.25");
        insertDeposit(1, "basic", 40, "100.00");
        insertDeposit(2, "student", 400, "200.50");

        List<TimeDeposit> result = deposits.findAll();

        assertThat(result).extracting(TimeDeposit::getId).containsExactly(1, 2, 3);
        assertThat(result).extracting(TimeDeposit::getPlanType).containsExactly("basic", "student", "premium");
        assertThat(result).extracting(TimeDeposit::getDays).containsExactly(40, 400, 50);
        assertThat(result).extracting(TimeDeposit::getBalance).containsExactly(100.00, 200.50, 300.25);
    }

    @Test
    void findAllForUpdateReturnsRowsInIdOrder() {
        insertDeposit(5, "basic", 31, "10.00");
        insertDeposit(2, "basic", 31, "20.00");
        insertDeposit(9, "basic", 31, "30.00");

        assertThat(deposits.findAllForUpdate()).extracting(TimeDeposit::getId).containsExactly(2, 5, 9);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rowLockAndBalanceWritesRequireATransaction() {
        assertThatThrownBy(() -> deposits.findAllForUpdate())
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> deposits.saveBalances(List.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> lock.tryAcquire())
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void saveBalancesRoundsToTwoDecimalsAndPersists() {
        insertDeposit(1, "basic", 31, "100.00");
        insertDeposit(2, "premium", 50, "200.00");
        insertDeposit(3, "student", 99, "300.00");

        deposits.saveBalances(List.of(
                new TimeDeposit(1, "basic", 100.125, 31),
                new TimeDeposit(2, "premium", 6.029999999999999, 50),
                new TimeDeposit(3, "student", 300.0, 99)));
        entityManager.flush();

        assertThat(jdbc.queryForObject("select balance from time_deposits where id = 1", BigDecimal.class))
                .isEqualByComparingTo("100.13");
        assertThat(jdbc.queryForObject("select balance from time_deposits where id = 2", BigDecimal.class))
                .isEqualByComparingTo("6.03");
        assertThat(jdbc.queryForObject("select balance from time_deposits where id = 3", BigDecimal.class))
                .isEqualByComparingTo("300.00");
    }

    @Test
    void savesBalancesOfRowsLockedInTheSameTransaction() {
        insertDeposit(1, "basic", 31, "1000.00");

        List<TimeDeposit> locked = deposits.findAllForUpdate();
        locked.get(0).setBalance(1000.83);
        deposits.saveBalances(locked);
        entityManager.flush();

        assertThat(jdbc.queryForObject("select balance from time_deposits where id = 1", BigDecimal.class))
                .isEqualByComparingTo("1000.83");
    }

    @Test
    void withdrawalsFindAllMapsAmountDateAndDepositId() {
        insertDeposit(1, "basic", 31, "100.00");
        insertDeposit(2, "basic", 31, "100.00");
        jdbc.update("insert into withdrawals (id, time_deposit_id, amount, date) values (2, 2, 20.50, date '2026-02-02')");
        jdbc.update("insert into withdrawals (id, time_deposit_id, amount, date) values (1, 1, 10.00, date '2026-01-01')");

        assertThat(withdrawals.findAll()).containsExactly(
                new Withdrawal(1, 1, new BigDecimal("10.00"), LocalDate.of(2026, 1, 1)),
                new Withdrawal(2, 2, new BigDecimal("20.50"), LocalDate.of(2026, 2, 2)));
    }

    @Test
    void databaseRejectsUnknownPlanType() {
        assertThatThrownBy(() -> insertDeposit(1, "gold", 31, "100.00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void interestRunLockIsAcquiredWhenFree() {
        assertThat(lock.tryAcquire()).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void interestRunLockFailsWhileAnotherConnectionHoldsTheKey() throws SQLException {
        try (Connection other = dataSource.getConnection()) {
            other.createStatement().execute("select pg_advisory_lock(" + PostgresInterestRunLock.LOCK_KEY + ")");
            try {
                assertThat(inTransaction(lock::tryAcquire)).isFalse();
            } finally {
                other.createStatement().execute("select pg_advisory_unlock(" + PostgresInterestRunLock.LOCK_KEY + ")");
            }
        }
        assertThat(inTransaction(lock::tryAcquire)).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void interestRunLockIsHeldUntilTheCallerCommits() throws SQLException {
        try (Connection other = dataSource.getConnection()) {
            Boolean otherSessionGotLockDuringRun = new TransactionTemplate(transactionManager).execute(status -> {
                assertThat(lock.tryAcquire()).isTrue();
                return tryLockAndRelease(other);
            });

            assertThat(otherSessionGotLockDuringRun).isFalse();
            assertThat(tryLockAndRelease(other)).isTrue();
        }
    }

    private static boolean tryLockAndRelease(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery("select pg_try_advisory_lock(" + PostgresInterestRunLock.LOCK_KEY + ")");
            result.next();
            boolean acquired = result.getBoolean(1);
            if (acquired) {
                statement.execute("select pg_advisory_unlock(" + PostgresInterestRunLock.LOCK_KEY + ")");
            }
            return acquired;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}

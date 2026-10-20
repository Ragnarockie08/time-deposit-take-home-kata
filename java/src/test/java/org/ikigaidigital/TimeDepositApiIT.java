package org.ikigaidigital;

import org.ikigaidigital.application.port.out.InterestRunLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TimeDepositApiIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InterestRunLock interestRunLock;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void startFromEmptyTables() {
        jdbc.update("delete from withdrawals");
        jdbc.update("delete from time_deposits");
    }

    @Test
    void getListsDepositsWithTheirWithdrawals() throws Exception {
        insertDeposit(1, "premium", 46, "1000.00");
        insertDeposit(2, "basic", 10, "500.00");
        jdbc.update("insert into withdrawals (id, time_deposit_id, amount, date) values (7, 1, 250.00, date '2026-03-15')");

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].planType").value("premium"))
                .andExpect(jsonPath("$[0].balance").value(1000.00))
                .andExpect(jsonPath("$[0].days").value(46))
                .andExpect(jsonPath("$[0].withdrawals[0].id").value(7))
                .andExpect(jsonPath("$[0].withdrawals[0].amount").value(250.00))
                .andExpect(jsonPath("$[0].withdrawals[0].date").value("2026-03-15"))
                .andExpect(jsonPath("$[1].withdrawals", hasSize(0)));
    }

    @Test
    void postAppliesOneMonthOfInterestToEveryDeposit() throws Exception {
        insertDeposit(1, "basic", 31, "1000.00");
        insertDeposit(2, "student", 366, "1000.00");
        insertDeposit(3, "premium", 46, "1000.00");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        assertThat(balanceOf(1)).isEqualByComparingTo("1000.83");
        assertThat(balanceOf(2)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(3)).isEqualByComparingTo("1004.17");
    }

    @Test
    void consecutivePostsEachApplyAnotherMonthFromTheStoredBalance() throws Exception {
        insertDeposit(1, "basic", 31, "1000.00");

        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());
        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        assertThat(balanceOf(1)).isEqualByComparingTo("1001.66");
    }

    @Test
    void postIsRejectedWhileAnotherRunHoldsTheLockAndAcceptedAfterIt() throws Exception {
        insertDeposit(1, "basic", 31, "1000.00");
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch finishOtherRun = new CountDownLatch(1);
        CompletableFuture<Void> otherRun = CompletableFuture.runAsync(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    assertThat(interestRunLock.tryAcquire()).isTrue();
                    lockHeld.countDown();
                    awaitQuietly(finishOtherRun);
                }));
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();

        mockMvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
        assertThat(balanceOf(1)).isEqualByComparingTo("1000.00");

        finishOtherRun.countDown();
        otherRun.get(10, TimeUnit.SECONDS);
        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());
        assertThat(balanceOf(1)).isEqualByComparingTo("1000.83");
    }

    private void insertDeposit(int id, String planType, int days, String balance) {
        jdbc.update("insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)",
                id, planType, days, new BigDecimal(balance));
    }

    private BigDecimal balanceOf(int id) {
        return jdbc.queryForObject("select balance from time_deposits where id = ?", BigDecimal.class, id);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package org.ikigaidigital.adapter.rest;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.in.GetTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.InterestRunInProgressException;
import org.ikigaidigital.application.port.in.TimeDepositWithWithdrawals;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeDepositController.class)
@Import(RestExceptionHandler.class)
class TimeDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTimeDepositsUseCase getTimeDepositsUseCase;

    @MockitoBean
    private UpdateBalancesUseCase updateBalancesUseCase;

    @Test
    void getReturnsDepositsWithWithdrawals() throws Exception {
        when(getTimeDepositsUseCase.getAllTimeDeposits()).thenReturn(List.of(
                new TimeDepositWithWithdrawals(
                        new TimeDeposit(7, "basic", 1000.1, 45),
                        List.of(new Withdrawal(3, 7, new BigDecimal("25.50"), LocalDate.of(2026, 3, 9))))));

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].planType").value("basic"))
                .andExpect(jsonPath("$[0].balance").value(1000.10))
                .andExpect(jsonPath("$[0].days").value(45))
                .andExpect(jsonPath("$[0].withdrawals[0].id").value(3))
                .andExpect(jsonPath("$[0].withdrawals[0].amount").value(25.50))
                .andExpect(jsonPath("$[0].withdrawals[0].date").value("2026-03-09"));
    }

    @Test
    void getRendersBalanceWithTwoDecimals() throws Exception {
        when(getTimeDepositsUseCase.getAllTimeDeposits()).thenReturn(List.of(
                new TimeDepositWithWithdrawals(new TimeDeposit(1, "student", 1000.1, 45), List.of())));

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"balance\":1000.10")));
    }

    @Test
    void postRunsTheUpdateAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isNoContent());

        verify(updateBalancesUseCase).updateAllBalances();
    }

    @Test
    void postReturnsConflictProblemWhenARunIsInProgress() throws Exception {
        doThrow(new InterestRunInProgressException()).when(updateBalancesUseCase).updateAllBalances();

        mockMvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Balance update in progress"));
    }
}

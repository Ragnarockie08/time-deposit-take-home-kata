package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.GetTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.InterestRunInProgressException;
import org.ikigaidigital.application.port.in.TimeDepositWithWithdrawals;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.ikigaidigital.application.port.out.InterestRunLock;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeDepositService implements UpdateBalancesUseCase, GetTimeDepositsUseCase {

    private final TimeDepositRepository timeDepositRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final InterestRunLock interestRunLock;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(TimeDepositRepository timeDepositRepository,
                              WithdrawalRepository withdrawalRepository,
                              InterestRunLock interestRunLock,
                              TimeDepositCalculator calculator) {
        this.timeDepositRepository = timeDepositRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.interestRunLock = interestRunLock;
        this.calculator = calculator;
    }

    @Override
    @Transactional
    public void updateAllBalances() {
        if (!interestRunLock.tryAcquire()) {
            throw new InterestRunInProgressException();
        }
        List<TimeDeposit> deposits = timeDepositRepository.findAllForUpdate();
        calculator.updateBalance(deposits);
        timeDepositRepository.saveBalances(deposits);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<TimeDepositWithWithdrawals> getAllTimeDeposits() {
        Map<Integer, List<Withdrawal>> withdrawalsByDeposit = withdrawalRepository.findAll().stream()
                .collect(Collectors.groupingBy(Withdrawal::timeDepositId));
        return timeDepositRepository.findAll().stream()
                .map(deposit -> new TimeDepositWithWithdrawals(
                        deposit, withdrawalsByDeposit.getOrDefault(deposit.getId(), List.of())))
                .toList();
    }
}

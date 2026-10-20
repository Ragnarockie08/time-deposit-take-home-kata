package org.ikigaidigital.adapter.rest;

import org.ikigaidigital.adapter.rest.model.PlanTypeDto;
import org.ikigaidigital.adapter.rest.model.TimeDepositDto;
import org.ikigaidigital.adapter.rest.model.WithdrawalDto;
import org.ikigaidigital.application.port.in.TimeDepositWithWithdrawals;
import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class TimeDepositDtoMapper {

    private TimeDepositDtoMapper() {
    }

    static TimeDepositDto toDto(TimeDepositWithWithdrawals source) {
        var deposit = source.deposit();
        return new TimeDepositDto(
                deposit.getId(),
                PlanTypeDto.fromValue(deposit.getPlanType()),
                BigDecimal.valueOf(deposit.getBalance()).setScale(2, RoundingMode.HALF_UP),
                deposit.getDays(),
                source.withdrawals().stream().map(TimeDepositDtoMapper::toDto).toList());
    }

    private static WithdrawalDto toDto(Withdrawal withdrawal) {
        return new WithdrawalDto(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}

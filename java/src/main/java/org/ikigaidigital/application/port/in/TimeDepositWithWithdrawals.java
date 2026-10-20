package org.ikigaidigital.application.port.in;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;

import java.util.List;

/** Read model: the shared TimeDeposit cannot gain a withdrawals field, so the two are combined here. */
public record TimeDepositWithWithdrawals(TimeDeposit deposit, List<Withdrawal> withdrawals) {

    public TimeDepositWithWithdrawals {
        withdrawals = List.copyOf(withdrawals);
    }
}

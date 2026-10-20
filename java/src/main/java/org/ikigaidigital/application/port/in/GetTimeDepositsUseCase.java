package org.ikigaidigital.application.port.in;

import java.util.List;

public interface GetTimeDepositsUseCase {

    List<TimeDepositWithWithdrawals> getAllTimeDeposits();
}

package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

public interface TimeDepositRepository {

    List<TimeDeposit> findAll();

    /**
     * Loads all deposits and locks their rows (in id order) until the surrounding transaction ends,
     * so concurrent writers cannot lose an update.
     */
    List<TimeDeposit> findAllForUpdate();

    /** Persists the balance of each given deposit, rounded to two decimal places. */
    void saveBalances(List<TimeDeposit> deposits);
}

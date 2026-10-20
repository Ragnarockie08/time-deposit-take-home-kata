package org.ikigaidigital.application.port.in;

public interface UpdateBalancesUseCase {

    /**
     * Applies one month of interest to every time deposit, atomically.
     *
     * @throws InterestRunInProgressException when another update is running on any instance
     */
    void updateAllBalances();
}

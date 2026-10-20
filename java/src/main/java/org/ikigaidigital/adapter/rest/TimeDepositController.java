package org.ikigaidigital.adapter.rest;

import org.ikigaidigital.adapter.rest.api.TimeDepositsApi;
import org.ikigaidigital.adapter.rest.model.TimeDepositDto;
import org.ikigaidigital.application.port.in.GetTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateBalancesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TimeDepositController implements TimeDepositsApi {

    private final GetTimeDepositsUseCase getTimeDepositsUseCase;
    private final UpdateBalancesUseCase updateBalancesUseCase;

    public TimeDepositController(GetTimeDepositsUseCase getTimeDepositsUseCase,
                                 UpdateBalancesUseCase updateBalancesUseCase) {
        this.getTimeDepositsUseCase = getTimeDepositsUseCase;
        this.updateBalancesUseCase = updateBalancesUseCase;
    }

    @Override
    public ResponseEntity<List<TimeDepositDto>> getTimeDeposits() {
        return ResponseEntity.ok(getTimeDepositsUseCase.getAllTimeDeposits().stream()
                .map(TimeDepositDtoMapper::toDto)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateBalances() {
        updateBalancesUseCase.updateAllBalances();
        return ResponseEntity.noContent().build();
    }
}

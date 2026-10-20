package org.ikigaidigital.adapter.db;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private final TimeDepositJpaRepository deposits;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository deposits) {
        this.deposits = deposits;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDeposit> findAll() {
        return deposits.findAllByOrderByIdAsc().stream().map(TimeDepositPersistenceAdapter::toDomain).toList();
    }

    /** Row locks last until the caller's transaction ends, so running without one is a programming error. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<TimeDeposit> findAllForUpdate() {
        return deposits.findAllForUpdate().stream().map(TimeDepositPersistenceAdapter::toDomain).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveBalances(List<TimeDeposit> toSave) {
        // One select loads all rows; changed balances are written by dirty checking at flush.
        Map<Integer, TimeDepositEntity> managed = deposits
                .findAllById(toSave.stream().map(TimeDeposit::getId).toList())
                .stream()
                .collect(Collectors.toMap(TimeDepositEntity::getId, Function.identity()));
        for (TimeDeposit deposit : toSave) {
            // The shared class keeps a Double; the database keeps cents.
            managed.get(deposit.getId())
                    .setBalance(BigDecimal.valueOf(deposit.getBalance()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(),
                entity.getDays());
    }
}

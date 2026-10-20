package org.ikigaidigital.adapter.db;

import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Separate from {@link TimeDepositPersistenceAdapter}: both ports declare {@code findAll()} with the same
 * erasure, so one class cannot implement them together.
 */
@Component
class WithdrawalPersistenceAdapter implements WithdrawalRepository {

    private final WithdrawalJpaRepository withdrawals;

    WithdrawalPersistenceAdapter(WithdrawalJpaRepository withdrawals) {
        this.withdrawals = withdrawals;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> findAll() {
        return withdrawals.findAllByOrderByIdAsc().stream()
                .map(entity -> new Withdrawal(entity.getId(), entity.getTimeDepositId(), entity.getAmount(),
                        entity.getDate()))
                .toList();
    }
}

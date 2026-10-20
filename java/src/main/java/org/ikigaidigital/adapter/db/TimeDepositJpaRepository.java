package org.ikigaidigital.adapter.db;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findAllByOrderByIdAsc();

    /** FOR NO KEY UPDATE on PostgreSQL; the fixed id order prevents deadlocks between runs. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from TimeDepositEntity d order by d.id")
    List<TimeDepositEntity> findAllForUpdate();
}

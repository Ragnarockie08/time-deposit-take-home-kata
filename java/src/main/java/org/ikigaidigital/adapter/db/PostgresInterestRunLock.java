package org.ikigaidigital.adapter.db;

import org.ikigaidigital.application.port.out.InterestRunLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Uses a PostgreSQL transaction-level advisory lock, so it works across application instances. */
@Component
class PostgresInterestRunLock implements InterestRunLock {

    /** Arbitrary fixed key identifying the interest run; every instance must use the same value. */
    static final long LOCK_KEY = 7_204_100_001L;

    private final JdbcTemplate jdbc;

    PostgresInterestRunLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Released by PostgreSQL at commit or rollback, so it needs the caller's transaction. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryAcquire() {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, LOCK_KEY));
    }
}

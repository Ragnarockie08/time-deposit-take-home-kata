package org.ikigaidigital.application.port.out;

/** A lock shared by all application instances that guards a balance update run. */
public interface InterestRunLock {

    /**
     * Tries to take the lock without waiting. It is held until the surrounding transaction ends.
     *
     * @return false when another run holds it
     */
    boolean tryAcquire();
}

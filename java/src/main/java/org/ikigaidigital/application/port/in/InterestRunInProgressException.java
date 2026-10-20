package org.ikigaidigital.application.port.in;

public class InterestRunInProgressException extends RuntimeException {

    public InterestRunInProgressException() {
        super("Another balance update is already running");
    }
}

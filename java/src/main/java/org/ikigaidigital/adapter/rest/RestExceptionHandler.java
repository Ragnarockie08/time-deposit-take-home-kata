package org.ikigaidigital.adapter.rest;

import org.ikigaidigital.application.port.in.InterestRunInProgressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InterestRunInProgressException.class)
    public ProblemDetail handleInterestRunInProgress(InterestRunInProgressException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Balance update in progress");
        return problem;
    }
}

package org.ikigaidigital.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A withdrawal from a time deposit. Withdrawals do not affect interest (not specified by the task). */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}

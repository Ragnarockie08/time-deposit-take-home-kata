package org.ikigaidigital.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The time deposit products. {@code code} is the value stored in the database and carried by the
 * shared {@link org.ikigaidigital.TimeDeposit} class, which keeps the plan as a String.
 */
public enum PlanType {
    BASIC("basic"),
    STUDENT("student"),
    PREMIUM("premium");

    private final String code;

    PlanType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Matching is case-sensitive, as in the original calculator. A null code is rejected, which keeps the
     * original behaviour of failing on a deposit without a plan.
     */
    public static Optional<PlanType> fromCode(String code) {
        Objects.requireNonNull(code, "planType");
        return Arrays.stream(values())
                .filter(planType -> planType.code.equals(code))
                .findFirst();
    }
}

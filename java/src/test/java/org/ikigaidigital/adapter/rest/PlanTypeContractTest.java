package org.ikigaidigital.adapter.rest;

import org.ikigaidigital.adapter.rest.model.PlanTypeDto;
import org.ikigaidigital.domain.PlanType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTypeContractTest {

    @Test
    void apiContractListsExactlyTheDomainPlanTypes() {
        assertThat(Arrays.stream(PlanTypeDto.values()).map(PlanTypeDto::getValue))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(PlanType.values()).map(PlanType::code).toList());
    }
}

/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class E2eGroupedByE2eMapperTest {

    private final E2eGroupedByE2eMapper mapper = new E2eGroupedByE2eMapper();

    @Test
    @DisplayName("Три id и вложения: заполненный id даёт archOperation*, пустой — null")
    void mapsArchOperationsForAllBranches() {
        DiscoveredOperation discovered = DiscoveredOperation.builder()
                .id(1)
                .interfaceId(2)
                .connectionOperationId(10)
                .connectionOperationDevId(20)
                .connectionOperationDesignId(null)
                .name("/orders")
                .type("GET")
                .build();
        Operation main = Operation.builder().id(10).name("main-op").type("GET").tcId(100).build();
        Operation dev = Operation.builder().id(20).name("dev-op").type("GET").tcId(200).build();
        TcDTO mainTc = TcDTO.builder().id(100).name("tc-main").build();
        TcDTO devTc = TcDTO.builder().id(200).name("tc-dev").build();

        E2eGroupedByE2eOperationDTO dto = mapper.toOperationDto(
                discovered,
                Map.of(10, main, 20, dev),
                Map.of(100, mainTc, 200, devTc));

        assertThat(dto.getConnectionOperationId()).isEqualTo(10);
        assertThat(dto.getConnectionOperationDevId()).isEqualTo(20);
        assertThat(dto.getConnectionOperationDesignId()).isNull();
        assertThat(dto.getArchOperation().getId()).isEqualTo(10);
        assertThat(dto.getArchOperation().getTechCapability().getId()).isEqualTo(100);
        assertThat(dto.getArchOperationDev().getId()).isEqualTo(20);
        assertThat(dto.getArchOperationDev().getTechCapability().getId()).isEqualTo(200);
        assertThat(dto.getArchOperationDesign()).isNull();
    }
}

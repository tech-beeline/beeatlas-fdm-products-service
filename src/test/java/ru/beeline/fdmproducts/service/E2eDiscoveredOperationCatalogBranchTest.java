/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.search.projection.DiscoveredOperationProjection;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class E2eDiscoveredOperationCatalogBranchTest {

    @Mock
    private OperationRepository operationRepository;
    @Mock
    private SlaRepository slaRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private ContainerRepository containerRepository;

    @InjectMocks
    private E2eDiscoveredOperationCatalog catalog;

    @Test
    @DisplayName("operation/operationDev/operationDesign по трём id; пустая связь — null")
    void buildsBranchOperations() {
        DiscoveredOperationProjection projection = mock(DiscoveredOperationProjection.class);
        when(projection.getOpId()).thenReturn(1);
        when(projection.getOpName()).thenReturn("/orders");
        when(projection.getOpType()).thenReturn("GET");
        when(projection.getInterfaceCode()).thenReturn("iface");
        when(projection.getProductAlias()).thenReturn("orders");
        when(projection.getSource()).thenReturn("SPARX");
        when(projection.getConnectionOperationId()).thenReturn(10);
        when(projection.getConnectionOperationDevId()).thenReturn(20);
        when(projection.getConnectionOperationDesignId()).thenReturn(null);

        Operation main = Operation.builder().id(10).name("main").type("GET").build();
        Operation dev = Operation.builder().id(20).name("dev").type("GET").build();
        when(operationRepository.findAllByIdIn(anyList())).thenReturn(List.of(main, dev));
        when(slaRepository.findAllByOperationIdIn(anyList())).thenReturn(List.of());

        List<E2eV2DiscoveredOperationCatalogItemDTO> items = catalog.build(List.of(1), List.of(projection));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getOperation().getId()).isEqualTo(10);
        assertThat(items.get(0).getOperationDev().getId()).isEqualTo(20);
        assertThat(items.get(0).getOperationDesign()).isNull();
    }
}

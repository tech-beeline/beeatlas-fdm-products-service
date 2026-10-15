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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2GetResponseDTO;
import ru.beeline.fdmproducts.dto.search.projection.DiscoveredOperationProjection;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class E2eV2GetByCodeServiceTest {

    private static final String CODE = "checkout-flow";
    private static final Integer E2E_ID = 1;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private E2eRepository e2eRepository;
    @Mock
    private OperationRelationRepository operationRelationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private SlaRepository slaRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private E2eOwnerService e2eOwnerService;

    @InjectMocks
    private E2eV2Service service;

    @Test
    @DisplayName("operation со sla, interface и container")
    void enrichesOperationWithSlaInterfaceAndContainer() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, 80.0, 40.0, 0.2);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, 8, "getItems", "GET", 3)));
        when(slaRepository.findAllByOperationIdIn(List.of(100)))
                .thenReturn(List.of(sla(15, 100, 100.0, 50.0, 0.1)));
        when(interfaceRepository.findAllByIdIn(List.of(8)))
                .thenReturn(List.of(archInterface(8, "cart-rest", 4)));
        when(containerRepository.findAllByIdIn(List.of(4)))
                .thenReturn(List.of(container(4, "cart-api")));

        E2eV2GetResponseDTO response = service.getByCode(CODE);
        E2eV2DiscoveredOperationCatalogItemDTO item = response.getDiscoveredOperations().get(0);

        assertThat(item.getOperation().getId()).isEqualTo(100);
        assertThat(item.getOperation().getName()).isEqualTo("getItems");
        assertThat(item.getOperation().getType()).isEqualTo("GET");
        assertThat(item.getOperation().getTcId()).isEqualTo(3);
        assertThat(item.getOperation().getSla().getId()).isEqualTo(15);
        assertThat(item.getOperation().getConnectedInterface().getCode()).isEqualTo("cart-rest");
        assertThat(item.getOperation().getConnectedInterface().getContainer().getCode()).isEqualTo("cart-api");
        assertThat(item.getSla().getRps()).isEqualTo(80.0);
        assertThat(response.getOperations()).isEmpty();
    }

    @Test
    @DisplayName("operation без sla")
    void operationWithoutSla() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, 8, "getItems", "GET", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8)))
                .thenReturn(List.of(archInterface(8, "cart-rest", null)));

        E2eV2DiscoveredOperationCatalogItemDTO item = service.getByCode(CODE).getDiscoveredOperations().get(0);

        assertThat(item.getOperation()).isNotNull();
        assertThat(item.getOperation().getSla()).isNull();
        assertThat(item.getOperation().getConnectedInterface().getContainer()).isNull();
        verify(containerRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("interface без container")
    void interfaceWithoutContainer() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, 8, "op", "POST", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8)))
                .thenReturn(List.of(archInterface(8, "no-container", null)));

        assertThat(service.getByCode(CODE).getDiscoveredOperations().get(0)
                .getOperation().getConnectedInterface().getContainer()).isNull();
        verify(containerRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("connection_operation_id пустой — operation = null")
    void nullConnectionGivesNullOperation() {
        stubE2eWithDiscovered(40);
        DiscoveredOperationProjection projection = projection(40, null, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(40)))
                .thenReturn(List.of(projection));

        assertThat(service.getByCode(CODE).getDiscoveredOperations().get(0).getOperation()).isNull();
        verify(operationRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("строка operation не найдена — operation = null")
    void missingOperationGivesNull() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of());

        assertThat(service.getByCode(CODE).getDiscoveredOperations().get(0).getOperation()).isNull();
        verify(slaRepository, never()).findAllByOperationIdIn(anyList());
    }

    @Test
    @DisplayName("Удалённые operation, interface и container входят в ответ")
    void deletedEntitiesStillReturned() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));

        Operation deletedOp = operation(100, 8, "deleted-op", "GET", 3);
        deletedOp.setDeletedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        Interface deletedIface = archInterface(8, "deleted-iface", 4);
        deletedIface.setDeletedDate(LocalDateTime.of(2026, 1, 2, 0, 0));
        ContainerProduct deletedContainer = container(4, "deleted-container");
        deletedContainer.setDeletedDate(new Date());

        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(deletedOp));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(deletedIface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(deletedContainer));

        E2eV2DiscoveredOperationCatalogItemDTO item = service.getByCode(CODE).getDiscoveredOperations().get(0);

        assertThat(item.getOperation().getId()).isEqualTo(100);
        assertThat(item.getOperation().getConnectedInterface().getId()).isEqualTo(8);
        assertThat(item.getOperation().getConnectedInterface().getContainer().getId()).isEqualTo(4);
    }

    @Test
    @DisplayName("Две discovered-операции с одним connection_operation_id — один вложенный объект")
    void sameConnectionSharedBetweenTwoDiscovered() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e()));
        when(operationRelationRepository.findAllByE2eId(E2E_ID)).thenReturn(List.of(
                relation(null, 20),
                relation(20, 40)));

        DiscoveredOperationProjection p20 = projection(20, 100, null, null, null);
        DiscoveredOperationProjection p40 = projection(40, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20, 40)))
                .thenReturn(List.of(p20, p40));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, null, "shared", "GET", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());

        List<E2eV2DiscoveredOperationCatalogItemDTO> items = service.getByCode(CODE).getDiscoveredOperations();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getOperation()).isSameAs(items.get(1).getOperation());
        assertThat(items.get(0).getOperation().getId()).isEqualTo(100);
    }

    @Test
    @DisplayName("Несколько sla — строка с наименьшим id")
    void choosesSlaWithMinimalId() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, null, "op", "GET", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of(
                sla(30, 100, 1.0, 1.0, 1.0),
                sla(10, 100, 2.0, 2.0, 2.0),
                sla(20, 100, 3.0, 3.0, 3.0)));

        assertThat(service.getByCode(CODE).getDiscoveredOperations().get(0).getOperation().getSla().getId())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("Сопоставленная операция не попадает в справочник operations")
    void connectedOperationNotAddedToOperationsCatalog() {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, null, "arch-only", "GET", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());

        E2eV2GetResponseDTO response = service.getByCode(CODE);

        assertThat(response.getDiscoveredOperations().get(0).getOperation().getId()).isEqualTo(100);
        assertThat(response.getOperations()).isEmpty();
    }

    @Test
    @DisplayName("Пустой discoveredOperations не читает operation")
    void emptyDiscoveredDoesNotReadOperation() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e()));
        when(operationRelationRepository.findAllByE2eId(E2E_ID)).thenReturn(List.of());

        assertThat(service.getByCode(CODE).getDiscoveredOperations()).isEmpty();
        verify(discoveredOperationRepository, never()).findDiscoveredOperationsProjection(anyList());
        verify(operationRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("SFDM-4244: GET v2 отдаёт пустой operations и узел без entityTypeRelatedOperation")
    void v2HasEmptyOperationsAndNoEntityTypeInNodes() throws Exception {
        stubE2eWithDiscovered(20);
        DiscoveredOperationProjection projection = projection(20, null, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(20)))
                .thenReturn(List.of(projection));

        E2eV2GetResponseDTO response = service.getByCode(CODE);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);

        assertThat(response.getOperations()).isEmpty();
        assertThat(response.getOperationsRelations()).singleElement()
                .satisfies(node -> assertThat(node.getRelatedOperationId()).isEqualTo(20));
        assertThat(json).doesNotContain("entityTypeRelatedOperation").contains("\"operations\":[]");
        verify(operationRepository, never()).findOperationsProjection(anyList());
    }

    @Test
    @DisplayName("SFDM-4244: одинаковые числа id operation и discovered_operation не склеиваются")
    void sameNumericIdsOfOperationAndDiscoveredStayApart() {
        stubE2eWithDiscovered(100);
        DiscoveredOperationProjection projection = projection(100, 100, null, null, null);
        when(discoveredOperationRepository.findDiscoveredOperationsProjection(List.of(100)))
                .thenReturn(List.of(projection));
        when(operationRepository.findAllByIdIn(List.of(100)))
                .thenReturn(List.of(operation(100, null, "arch", "GET", null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());

        E2eV2GetResponseDTO response = service.getByCode(CODE);

        assertThat(response.getDiscoveredOperations()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(100);
            assertThat(item.getName()).isEqualTo("name-100");
            assertThat(item.getOperation().getName()).isEqualTo("arch");
        });
        assertThat(response.getOperations()).isEmpty();
    }

    private void stubE2eWithDiscovered(Integer discoveredId) {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e()));
        when(operationRelationRepository.findAllByE2eId(E2E_ID)).thenReturn(List.of(
                relation(null, discoveredId)));
    }

    private E2e e2e() {
        return E2e.builder().id(E2E_ID).code(CODE).name("E2E").source("SPARX").build();
    }

    private OperationRelation relation(Integer operationId, Integer relatedOperationId) {
        return OperationRelation.builder()
                .e2eId(E2E_ID)
                .operationId(operationId)
                .relatedOperationId(relatedOperationId)
                .order(1)
                .stereoType("sparx")
                .build();
    }

    private DiscoveredOperationProjection projection(Integer id, Integer connectionOperationId,
                                                     Double rps, Double latency, Double errorRate) {
        DiscoveredOperationProjection projection = mock(DiscoveredOperationProjection.class);
        when(projection.getOpId()).thenReturn(id);
        when(projection.getOpName()).thenReturn("name-" + id);
        when(projection.getOpType()).thenReturn("GET");
        when(projection.getInterfaceCode()).thenReturn("iface-" + id);
        when(projection.getProductAlias()).thenReturn("PRODUCT");
        when(projection.getSource()).thenReturn("SPARX");
        when(projection.getRps()).thenReturn(rps);
        when(projection.getLatency()).thenReturn(latency);
        when(projection.getErrorRate()).thenReturn(errorRate);
        when(projection.getConnectionOperationId()).thenReturn(connectionOperationId);
        return projection;
    }

    private Operation operation(Integer id, Integer interfaceId, String name, String type, Integer tcId) {
        Operation operation = new Operation();
        operation.setId(id);
        operation.setInterfaceId(interfaceId);
        operation.setName(name);
        operation.setType(type);
        operation.setTcId(tcId);
        return operation;
    }

    private Sla sla(Integer id, Integer operationId, Double rps, Double latency, Double errorRate) {
        Sla sla = new Sla();
        sla.setId(id);
        sla.setOperationId(operationId);
        sla.setRps(rps);
        sla.setLatency(latency);
        sla.setErrorRate(errorRate);
        return sla;
    }

    private Interface archInterface(Integer id, String code, Integer containerId) {
        Interface archInterface = new Interface();
        archInterface.setId(id);
        archInterface.setCode(code);
        archInterface.setContainerId(containerId);
        return archInterface;
    }

    private ContainerProduct container(Integer id, String code) {
        ContainerProduct container = new ContainerProduct();
        container.setId(id);
        container.setCode(code);
        return container;
    }
}

/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.E2eBranch;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV3GetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV3RelationTreeNodeDTO;
import ru.beeline.fdmproducts.dto.search.projection.DiscoveredOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eBranchRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SFDM-4239: GET /api/v3/e2e/{code}. */
class E2eV3ServiceTest {

    private static final String CODE = "checkout-flow";
    private static final Integer E2E_ID = 1;

    private E2eRepository e2eRepository;
    private OperationRelationRepository operationRelationRepository;
    private DiscoveredOperationRepository discoveredOperationRepository;
    private OperationRepository operationRepository;
    private SlaRepository slaRepository;
    private InterfaceRepository interfaceRepository;
    private ContainerRepository containerRepository;
    private ProductRepository productRepository;
    private E2eBranchRepository e2eBranchRepository;
    private E2eV3Service service;

    @BeforeEach
    void setUp() {
        e2eRepository = mock(E2eRepository.class);
        operationRelationRepository = mock(OperationRelationRepository.class);
        discoveredOperationRepository = mock(DiscoveredOperationRepository.class);
        operationRepository = mock(OperationRepository.class);
        slaRepository = mock(SlaRepository.class);
        interfaceRepository = mock(InterfaceRepository.class);
        containerRepository = mock(ContainerRepository.class);
        productRepository = mock(ProductRepository.class);
        e2eBranchRepository = mock(E2eBranchRepository.class);
        // Ветка main карточки получила id карточки (SFDM-4250).
        when(e2eBranchRepository.findAllByCodeAndBranchNameIgnoreCaseOrderByIdAsc(CODE, "main"))
                .thenReturn(List.of(E2eBranch.builder().id(E2E_ID).code(CODE).branchName("main").build()));
        service = new E2eV3Service(e2eRepository,
                new E2eBranchService(e2eBranchRepository, operationRelationRepository), discoveredOperationRepository,
                new E2eDiscoveredOperationCatalog(operationRepository, slaRepository, interfaceRepository,
                        containerRepository),
                new E2eOwnerService(productRepository));
    }

    @Test
    @DisplayName("Дерево из примера постановки: корень 20, ребёнок 40, operation = null у 40, ключа operations нет")
    void buildsTheExampleTree() throws Exception {
        E2e e2e = e2e(7);
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e));
        when(productRepository.findAllById(List.of(7)))
                .thenReturn(List.of(Product.builder().id(7).alias("CHECKOUT").build()));
        givenRelations(relation(1, null, 20, 1, "sparx"), relation(2, 20, 40, 1, null));
        givenCards(card(20, "/items", "GET", "cart-iface-guid", "CART", 100, 80.0, 40.0, 0.2),
                card(40, "/notify", "POST", "notify-iface-guid", "NOTIFY", null, null, null, null));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation(100, "getItems", 8)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of(sla(15, 100)));
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(iface(8, "cart-rest", 4)));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container(4, "cart-api")));

        E2eV3GetResponseDTO response = service.getByCode(CODE, null);
        JsonNode json = new ObjectMapper().valueToTree(response);

        assertThat(json.has("operations")).isFalse();
        assertThat(json.path("e2e").path("productId").asInt()).isEqualTo(7);
        assertThat(json.path("e2e").path("productAlias").asText()).isEqualTo("CHECKOUT");
        assertThat(json.path("e2e").path("biStepCode").asText()).isEqualTo("BI-STEP-42");

        JsonNode root = json.path("operationsRelations").get(0);
        assertThat(root.path("relatedOperationId").asInt()).isEqualTo(20);
        assertThat(root.path("stereotype").asText()).isEqualTo("sparx");
        assertThat(root.has("entityTypeRelatedOperation")).isFalse();
        JsonNode child = root.path("operationsRelations").get(0);
        assertThat(child.path("relatedOperationId").asInt()).isEqualTo(40);
        assertThat(child.path("stereotype").isNull()).isTrue();
        assertThat(child.path("operationsRelations")).isEmpty();

        JsonNode items = json.path("discoveredOperations");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).path("sla").path("rps").asDouble()).isEqualTo(80.0);
        JsonNode operation = items.get(0).path("operation");
        assertThat(operation.path("id").asInt()).isEqualTo(100);
        assertThat(operation.path("tcId").asInt()).isEqualTo(3);
        assertThat(operation.has("interfaceId")).isFalse();
        assertThat(operation.has("deletedDate")).isFalse();
        assertThat(operation.path("sla").path("id").asInt()).isEqualTo(15);
        assertThat(operation.path("interface").path("code").asText()).isEqualTo("cart-rest");
        assertThat(operation.path("interface").path("container").path("code").asText()).isEqualTo("cart-api");
        assertThat(items.get(1).path("sla").isNull()).isTrue();
        assertThat(items.get(1).path("operation").isNull()).isTrue();
    }

    @Test
    @DisplayName("Два шага с одним order — раньше связь с меньшим id; пустой stereotype — null")
    void sortsByOrderThenRelationId() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(9, null, 30, 1, ""), relation(3, null, 20, 1, "  "), relation(1, null, 50, 2, "x"));
        givenCards(card(20), card(30), card(50));

        E2eV3GetResponseDTO response = service.getByCode(CODE, null);

        assertThat(response.getOperationsRelations()).extracting(E2eV3RelationTreeNodeDTO::getRelatedOperationId)
                .containsExactly(20, 30, 50);
        assertThat(response.getOperationsRelations()).extracting(E2eV3RelationTreeNodeDTO::getStereotype)
                .containsExactly(null, null, "x");
        assertThat(response.getDiscoveredOperations()).extracting(E2eV2DiscoveredOperationCatalogItemDTO::getId)
                .containsExactly(20, 30, 50);
    }

    @Test
    @DisplayName("Цикл по id discovered_operation обрывает ветку, справочник без повторов")
    void cycleStopsTheBranch() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(1, null, 20, 1, null), relation(2, 20, 40, 1, null), relation(3, 40, 20, 1, null));
        givenCards(card(20), card(40));

        E2eV3GetResponseDTO response = service.getByCode(CODE, null);

        E2eV3RelationTreeNodeDTO back = response.getOperationsRelations().get(0)
                .getOperationsRelations().get(0).getOperationsRelations().get(0);
        assertThat(back.getRelatedOperationId()).isEqualTo(20);
        assertThat(back.getOperationsRelations()).isEmpty();
        assertThat(response.getDiscoveredOperations()).extracting(E2eV2DiscoveredOperationCatalogItemDTO::getId)
                .containsExactly(20, 40);
    }

    @Test
    @DisplayName("Пустой product_id — productId и productAlias равны null, владелец не читается")
    void emptyOwnerGivesNullOwnerFields() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations();

        E2eV3GetResponseDTO response = service.getByCode(CODE, null);

        assertThat(response.getE2e().getProductId()).isNull();
        assertThat(response.getE2e().getProductAlias()).isNull();
        verify(productRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Нет e2e — 404 с сообщением постановки")
    void unknownCodeIsNotFound() {
        when(e2eRepository.findByCodeIgnoreCase("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode("nope", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("E2e с указанным кодом не найден");
    }

    @Test
    @DisplayName("Нет связей — 200, пустые массивы и карточка на месте")
    void noRelationsGivesEmptyArrays() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations();

        E2eV3GetResponseDTO response = service.getByCode(CODE, null);

        assertThat(response.getE2e().getCode()).isEqualTo(CODE);
        assertThat(response.getOperationsRelations()).isEmpty();
        assertThat(response.getDiscoveredOperations()).isEmpty();
        verify(discoveredOperationRepository, never()).findDiscoveredOperationCards(any());
    }

    @Test
    @DisplayName("Удалённые operation, interface и container входят во вложенный объект")
    void deletedArchitectureIsStillReturned() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(1, null, 20, 1, null));
        givenCards(card(20, "/items", "GET", "c", "CART", 100, null, null, null));
        Operation deleted = operation(100, "gone", 8);
        deleted.setDeletedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        Interface deletedIface = iface(8, "gone-iface", 4);
        deletedIface.setDeletedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        ContainerProduct deletedContainer = container(4, "gone-container");
        deletedContainer.setDeletedDate(new java.util.Date());
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(deleted));
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(deletedIface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(deletedContainer));

        E2eV2DiscoveredOperationCatalogItemDTO item = service.getByCode(CODE, null).getDiscoveredOperations().get(0);

        assertThat(item.getOperation().getName()).isEqualTo("gone");
        assertThat(item.getOperation().getConnectedInterface().getCode()).isEqualTo("gone-iface");
        assertThat(item.getOperation().getConnectedInterface().getContainer().getCode()).isEqualTo("gone-container");
        assertThat(item.getOperation().getSla()).isNull();
    }

    @Test
    @DisplayName("Два discovered с одним connection_operation_id получают один и тот же operation")
    void sharedConnectionGivesTheSameNestedObject() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(1, null, 20, 1, null), relation(2, 20, 40, 1, null));
        givenCards(card(20, "/a", "GET", "c", "P", 100, null, null, null),
                card(40, "/b", "GET", "c", "P", 100, null, null, null));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation(100, "shared", null)));

        List<E2eV2DiscoveredOperationCatalogItemDTO> items = service.getByCode(CODE, null).getDiscoveredOperations();

        assertThat(items.get(0).getOperation()).isSameAs(items.get(1).getOperation());
        verify(operationRepository).findAllByIdIn(List.of(100));
        verify(interfaceRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("Пустой connection_operation_id не читает product.operation, sla, interface и containers_product")
    void emptyConnectionsDoNotReadArchitecture() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(1, null, 40, 1, null));
        givenCards(card(40));

        assertThat(service.getByCode(CODE, null).getDiscoveredOperations().get(0).getOperation()).isNull();
        verify(operationRepository, never()).findAllByIdIn(anyList());
        verify(slaRepository, never()).findAllByOperationIdIn(anyList());
        verify(interfaceRepository, never()).findAllByIdIn(anyList());
        verify(containerRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("connection_operation_id без строки product.operation — operation = null, карточка остаётся")
    void missingArchitectureRowKeepsTheCard() {
        when(e2eRepository.findByCodeIgnoreCase(CODE)).thenReturn(Optional.of(e2e(null)));
        givenRelations(relation(1, null, 20, 1, null));
        givenCards(card(20, "/a", "GET", "c", "P", 100, null, null, null));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of());

        List<E2eV2DiscoveredOperationCatalogItemDTO> items = service.getByCode(CODE, null).getDiscoveredOperations();

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(20);
            assertThat(item.getOperation()).isNull();
        });
    }

    private void givenRelations(OperationRelation... relations) {
        when(operationRelationRepository.findAllByE2eBranchId(E2E_ID)).thenReturn(new ArrayList<>(Arrays.asList(relations)));
    }

    private void givenCards(DiscoveredOperationProjection... cards) {
        when(discoveredOperationRepository.findDiscoveredOperationCards(any())).thenReturn(List.of(cards));
    }

    private static E2e e2e(Integer productId) {
        return E2e.builder().id(E2E_ID).code(CODE).name("Оформление заказа").description("E2E от корзины до оплаты")
                .biStepCode("BI-STEP-42").source("SPARX").productId(productId).build();
    }

    private static OperationRelation relation(Integer id, Integer operationId, Integer relatedOperationId,
                                              Integer order, String stereotype) {
        return OperationRelation.builder().id(id).e2eBranchId(E2E_ID).operationId(operationId)
                .relatedOperationId(relatedOperationId).order(order).stereoType(stereotype).build();
    }

    private static DiscoveredOperationProjection card(Integer id) {
        return card(id, "name-" + id, "GET", "iface-" + id, "P", null, null, null, null);
    }

    private static DiscoveredOperationProjection card(Integer id, String name, String type, String interfaceCode,
                                                      String productAlias, Integer connectionOperationId,
                                                      Double rps, Double latency, Double errorRate) {
        DiscoveredOperationProjection projection = mock(DiscoveredOperationProjection.class);
        when(projection.getOpId()).thenReturn(id);
        when(projection.getOpName()).thenReturn(name);
        when(projection.getOpType()).thenReturn(type);
        when(projection.getInterfaceCode()).thenReturn(interfaceCode);
        when(projection.getProductAlias()).thenReturn(productAlias);
        when(projection.getSource()).thenReturn("SPARX");
        when(projection.getRps()).thenReturn(rps);
        when(projection.getLatency()).thenReturn(latency);
        when(projection.getErrorRate()).thenReturn(errorRate);
        when(projection.getConnectionOperationId()).thenReturn(connectionOperationId);
        return projection;
    }

    private static Operation operation(Integer id, String name, Integer interfaceId) {
        Operation operation = new Operation();
        operation.setId(id);
        operation.setName(name);
        operation.setType("GET");
        operation.setTcId(3);
        operation.setInterfaceId(interfaceId);
        return operation;
    }

    private static Sla sla(Integer id, Integer operationId) {
        Sla sla = new Sla();
        sla.setId(id);
        sla.setOperationId(operationId);
        sla.setRps(100.0);
        sla.setLatency(50.0);
        sla.setErrorRate(0.1);
        return sla;
    }

    private static Interface iface(Integer id, String code, Integer containerId) {
        Interface iface = new Interface();
        iface.setId(id);
        iface.setCode(code);
        iface.setContainerId(containerId);
        return iface;
    }

    private static ContainerProduct container(Integer id, String code) {
        ContainerProduct container = new ContainerProduct();
        container.setId(id);
        container.setCode(code);
        return container;
    }
}

/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SFDM-4319: comparison_operations сопоставляет discovered-операцию по веткам main, develop и design. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComparisonOperationsServiceTest {

    private static final String ALIAS = "orders-product";
    private static final List<Integer> MAIN_INTERFACES = List.of(101);
    private static final List<Integer> DEVELOP_INTERFACES = List.of(201);
    private static final List<Integer> DESIGN_INTERFACES = List.of(301);

    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private ArchOperationMatchingService archOperationMatchingService;

    @InjectMocks
    private ComparisonOperationsService service;

    private DiscoveredOperation discoveredOperation;
    private DiscoveredInterface discoveredInterface;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setAlias(ALIAS);
        discoveredInterface = new DiscoveredInterface();
        discoveredInterface.setId(3);
        discoveredInterface.setContext("/ctx");
        discoveredInterface.setProduct(product);

        discoveredOperation = new DiscoveredOperation();
        discoveredOperation.setId(9);
        discoveredOperation.setInterfaceId(3);
        discoveredOperation.setName("/orders");
        discoveredOperation.setContext("/v1");
        discoveredOperation.setType("GET");
        discoveredOperation.setDiscoveredInterface(discoveredInterface);
        discoveredInterface.setOperations(List.of(discoveredOperation));

        when(discoveredOperationRepository.findById(9)).thenReturn(Optional.of(discoveredOperation));
        when(discoveredOperationRepository.findAllByInterfaceIdAndDeletedDateIsNull(3))
                .thenReturn(List.of(discoveredOperation));
        when(archOperationMatchingService.resolveInterfaceIds(ALIAS, "main")).thenReturn(MAIN_INTERFACES);
        when(archOperationMatchingService.resolveInterfaceIds(ALIAS, "develop")).thenReturn(DEVELOP_INTERFACES);
        when(archOperationMatchingService.resolveInterfaceIds(ALIAS, "design")).thenReturn(DESIGN_INTERFACES);
        when(archOperationMatchingService.findFirstMatch(anyString(), any(), anyList())).thenReturn(Optional.empty());
    }

    private void givenMatch(String name, List<Integer> interfaceIds, int operationId, int interfaceId) {
        ArchOperationProjection projection = mock(ArchOperationProjection.class);
        when(projection.getOpId()).thenReturn(operationId);
        when(projection.getInterfaceId()).thenReturn(interfaceId);
        when(archOperationMatchingService.findFirstMatch(name, "GET", interfaceIds)).thenReturn(Optional.of(projection));
    }

    private void givenArchInterfaceOperations(int interfaceId, Integer... operationIds) {
        when(operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(interfaceId)).thenReturn(
                Arrays.stream(operationIds).map(id -> Operation.builder().id(id).build()).toList());
    }

    @Test
    @DisplayName("Одно событие заполняет атрибуты main, develop и design разными операциями своих веток")
    void fillsAllBranchesInOneEvent() {
        givenMatch("/orders", MAIN_INTERFACES, 11, 101);
        givenMatch("/v1/orders", DEVELOP_INTERFACES, 21, 201);
        givenMatch("/ctx/orders", DESIGN_INTERFACES, 31, 301);
        givenArchInterfaceOperations(101, 11);
        givenArchInterfaceOperations(201, 21);
        givenArchInterfaceOperations(301, 31);

        service.process(9);

        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(11);
        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(21);
        assertThat(discoveredOperation.getConnectionOperationDesignId()).isEqualTo(31);
        assertThat(discoveredInterface.getConnectionInterfaceId()).isEqualTo(101);
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isEqualTo(201);
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isEqualTo(301);
    }

    @Test
    @DisplayName("Совпадение develop на попытке 1 не запускает попытки 2 и 3 для develop и не мешает main")
    void stopsAttemptsPerBranch() {
        givenMatch("/orders", DEVELOP_INTERFACES, 21, 201);
        givenMatch("/v1/orders", MAIN_INTERFACES, 11, 101);
        givenArchInterfaceOperations(101, 11);
        givenArchInterfaceOperations(201, 21);

        service.process(9);

        verify(archOperationMatchingService, never()).findFirstMatch("/v1/orders", "GET", DEVELOP_INTERFACES);
        verify(archOperationMatchingService, never()).findFirstMatch("/ctx/orders", "GET", DEVELOP_INTERFACES);
        verify(archOperationMatchingService).findFirstMatch("/orders", "GET", MAIN_INTERFACES);
        verify(archOperationMatchingService).findFirstMatch("/ctx/orders", "GET", DESIGN_INTERFACES);
        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(11);
        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(21);
    }

    @Test
    @DisplayName("Ветка без совпадения и без интерфейсов оставляет свои атрибуты как были")
    void branchWithoutMatchKeepsAttributes() {
        discoveredOperation.setConnectionOperationDevId(55);
        discoveredInterface.setConnectionInterfaceDevId(205);
        when(archOperationMatchingService.resolveInterfaceIds(ALIAS, "design")).thenReturn(List.of());
        givenMatch("/orders", MAIN_INTERFACES, 11, 101);
        givenArchInterfaceOperations(101, 11);

        service.process(9);

        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(11);
        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(55);
        assertThat(discoveredOperation.getConnectionOperationDesignId()).isNull();
        assertThat(discoveredInterface.getConnectionInterfaceId()).isEqualTo(101);
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isEqualTo(205);
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isNull();
        verify(archOperationMatchingService, never()).findFirstMatch(anyString(), any(), eq(List.of()));
        verify(operationRepository, never()).findAllByInterfaceIdAndDeletedDateIsNull(201);
    }

    @Test
    @DisplayName("Связь интерфейса ветки не пишется, если не все методы ссылаются атрибутом ветки на его операции")
    void interfaceNotConnectedWhenOtherMethodIsUnmatchedInBranch() {
        DiscoveredOperation other = new DiscoveredOperation();
        other.setId(10);
        other.setConnectionOperationId(12);
        when(discoveredOperationRepository.findAllByInterfaceIdAndDeletedDateIsNull(3))
                .thenReturn(List.of(discoveredOperation, other));
        givenMatch("/orders", DEVELOP_INTERFACES, 21, 201);
        givenArchInterfaceOperations(201, 21, 22);

        service.process(9);

        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(21);
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isNull();
        verify(discoveredInterfaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Нет совпадения ни в одной ветке — ничего не сохраняется")
    void noMatchSavesNothing() {
        service.process(9);

        verify(discoveredOperationRepository, never()).save(any());
        verify(discoveredInterfaceRepository, never()).save(any());
    }
}

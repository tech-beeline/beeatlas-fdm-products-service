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
import ru.beeline.fdmproducts.domain.DiscoveredConnectionBranch;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchContainerRelationsServiceTest {

    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private ProductBranchService productBranchService;

    @InjectMocks
    private ArchContainerRelationsService service;

    @Test
    @DisplayName("Удаление контейнера main сбрасывает связи ветки main")
    void mainContainerDeleteClearsRelations() {
        when(productBranchService.findConnectionBranchByContainerId(10))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.MAIN));

        service.processContainerDelete(10);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByEntityId(10, "main");
        verify(discoveredOperationRepository).clearConnectionOperationIdByEntityId(10, "main");
    }

    @Test
    @DisplayName("Удаление контейнера develop и design сбрасывает связи только своей ветки")
    void developAndDesignContainerDeleteClearsOwnBranchRelations() {
        when(productBranchService.findConnectionBranchByContainerId(10))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DEVELOP));
        when(productBranchService.findConnectionBranchByContainerId(11))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DESIGN));

        service.processContainerDelete(10);
        service.processContainerDelete(11);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByEntityId(10, "develop");
        verify(discoveredOperationRepository).clearConnectionOperationIdByEntityId(10, "develop");
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByEntityId(11, "design");
        verify(discoveredOperationRepository).clearConnectionOperationIdByEntityId(11, "design");
        verifyNoMoreInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("Удаление контейнера не из main, develop или design связи не трогает")
    void nonMainContainerDeleteKeepsRelations() {
        when(productBranchService.findConnectionBranchByContainerId(anyInt())).thenReturn(Optional.empty());

        service.processContainerDelete(10);

        verifyNoInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("Удаление интерфейса main сбрасывает связи ветки main")
    void mainInterfaceDeleteClearsRelations() {
        when(productBranchService.findConnectionBranchByInterfaceId(20))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.MAIN));

        service.processInterfaceDelete(20);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByInterfaceId(20, "main");
        verify(discoveredOperationRepository).clearConnectionOperationIdByInterfaceId(20, "main");
    }

    @Test
    @DisplayName("Удаление интерфейса develop и design сбрасывает связи только своей ветки")
    void developAndDesignInterfaceDeleteClearsOwnBranchRelations() {
        when(productBranchService.findConnectionBranchByInterfaceId(20))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DEVELOP));
        when(productBranchService.findConnectionBranchByInterfaceId(21))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DESIGN));

        service.processInterfaceDelete(20);
        service.processInterfaceDelete(21);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByInterfaceId(20, "develop");
        verify(discoveredOperationRepository).clearConnectionOperationIdByInterfaceId(20, "develop");
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByInterfaceId(21, "design");
        verify(discoveredOperationRepository).clearConnectionOperationIdByInterfaceId(21, "design");
        verifyNoMoreInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("Удаление интерфейса не из main, develop или design связи не трогает")
    void nonMainInterfaceDeleteKeepsRelations() {
        when(productBranchService.findConnectionBranchByInterfaceId(anyInt())).thenReturn(Optional.empty());

        service.processInterfaceDelete(20);

        verifyNoInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("Удаление операции main сбрасывает связи")
    void mainOperationDeleteClearsRelations() {
        when(productBranchService.isOperationInDefaultBranch(30)).thenReturn(true);

        service.processOperationDelete(30);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByOperationId(30);
        verify(discoveredOperationRepository).clearConnectionOperationIdByOperationId(30);
    }

    @Test
    @DisplayName("Удаление операции не из main связи не трогает")
    void nonMainOperationDeleteKeepsRelations() {
        when(productBranchService.isOperationInDefaultBranch(anyInt())).thenReturn(false);

        service.processOperationDelete(30);

        verifyNoInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    /** Arch-операция 77 интерфейса 5 контейнера 4 продукта 41 и discovered-интерфейс 3 с одним методом 9. */
    private DiscoveredInterface givenArchOperationComparison(DiscoveredConnectionBranch connectionBranch,
                                                             DiscoveredOperation discoveredOperation) {
        Operation archOperation = new Operation();
        archOperation.setId(77);
        archOperation.setName("/api/v1/product/{code}");
        archOperation.setType("GET");
        archOperation.setInterfaceId(5);

        DiscoveredInterface discoveredInterface = new DiscoveredInterface();
        discoveredInterface.setId(3);
        discoveredInterface.setName("product-api");
        discoveredInterface.setOperations(List.of(discoveredOperation));

        Interface archInterface = new Interface();
        archInterface.setId(5);
        archInterface.setContainerId(4);

        Product product = new Product();
        product.setId(41);
        ProductBranch branch = new ProductBranch();
        branch.setProduct(product);
        ContainerProduct container = new ContainerProduct();
        container.setId(4);
        container.setProductBranch(branch);

        when(productBranchService.findConnectionBranchByOperationId(77)).thenReturn(Optional.of(connectionBranch));
        when(operationRepository.findById(77)).thenReturn(Optional.of(archOperation));
        when(interfaceRepository.findById(5)).thenReturn(Optional.of(archInterface));
        when(containerRepository.findById(4)).thenReturn(Optional.of(container));
        when(discoveredInterfaceRepository.findAllForArchOperationComparison(41, 5, connectionBranch.getBranchName()))
                .thenReturn(List.of(discoveredInterface));
        when(operationRepository.findAllByIdIn(List.of(77))).thenReturn(List.of(archOperation));
        return discoveredInterface;
    }

    private DiscoveredOperation discoveredOperation(String name) {
        DiscoveredOperation discoveredOperation = new DiscoveredOperation();
        discoveredOperation.setId(9);
        discoveredOperation.setName(name);
        discoveredOperation.setType("get");
        return discoveredOperation;
    }

    @Test
    @DisplayName("Разные имена path-параметров сопоставляются общим правилом")
    void matchesDiscoveredOperationWithADifferentParameterName() {
        DiscoveredOperation discoveredOperation = discoveredOperation("/api/v1/product/{cmdb}");
        givenArchOperationComparison(DiscoveredConnectionBranch.MAIN, discoveredOperation);

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(77);
    }

    @Test
    @DisplayName("Операция main пишет атрибуты main, связи develop и design не меняются")
    void mainOperationWritesMainAttributesOnly() {
        DiscoveredOperation discoveredOperation = discoveredOperation("/api/v1/product/{cmdb}");
        discoveredOperation.setConnectionOperationDevId(100);
        discoveredOperation.setConnectionOperationDesignId(200);
        DiscoveredInterface discoveredInterface =
                givenArchOperationComparison(DiscoveredConnectionBranch.MAIN, discoveredOperation);
        discoveredInterface.setConnectionInterfaceDevId(10);
        discoveredInterface.setConnectionInterfaceDesignId(20);

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(77);
        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(100);
        assertThat(discoveredOperation.getConnectionOperationDesignId()).isEqualTo(200);
        assertThat(discoveredInterface.getConnectionInterfaceId()).isEqualTo(5);
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isEqualTo(10);
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isEqualTo(20);
    }

    @Test
    @DisplayName("Операция develop перезаписывает атрибуты develop, атрибуты main и design не меняются")
    void developOperationWritesDevelopAttributesOnly() {
        DiscoveredOperation discoveredOperation = discoveredOperation("/api/v1/product/{cmdb}");
        discoveredOperation.setConnectionOperationId(100);
        discoveredOperation.setConnectionOperationDevId(55);
        DiscoveredInterface discoveredInterface =
                givenArchOperationComparison(DiscoveredConnectionBranch.DEVELOP, discoveredOperation);
        discoveredInterface.setConnectionInterfaceId(10);

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(77);
        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(100);
        assertThat(discoveredOperation.getConnectionOperationDesignId()).isNull();
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isEqualTo(5);
        assertThat(discoveredInterface.getConnectionInterfaceId()).isEqualTo(10);
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isNull();
        verify(discoveredInterfaceRepository).findAllForArchOperationComparison(41, 5, "develop");
    }

    @Test
    @DisplayName("Операция design пишет только атрибуты design")
    void designOperationWritesDesignAttributesOnly() {
        DiscoveredOperation discoveredOperation = discoveredOperation("/api/v1/product/{cmdb}");
        DiscoveredInterface discoveredInterface =
                givenArchOperationComparison(DiscoveredConnectionBranch.DESIGN, discoveredOperation);

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationDesignId()).isEqualTo(77);
        assertThat(discoveredOperation.getConnectionOperationId()).isNull();
        assertThat(discoveredOperation.getConnectionOperationDevId()).isNull();
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isEqualTo(5);
        assertThat(discoveredInterface.getConnectionInterfaceId()).isNull();
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isNull();
    }

    @Test
    @DisplayName("Метод без совпадения сохраняет связь своей ветки, интерфейс своей ветки не меняется")
    void unmatchedOperationKeepsBranchAttribute() {
        DiscoveredOperation discoveredOperation = discoveredOperation("/api/v1/other");
        discoveredOperation.setConnectionOperationDevId(55);
        DiscoveredInterface discoveredInterface =
                givenArchOperationComparison(DiscoveredConnectionBranch.DEVELOP, discoveredOperation);
        when(operationRepository.findAllByIdIn(List.of(55))).thenReturn(List.of());

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationDevId()).isEqualTo(55);
        assertThat(discoveredInterface.getConnectionInterfaceDevId()).isNull();
    }

    @Test
    @DisplayName("Метод без связи своей ветки не даёт связать интерфейс, даже если связь main заполнена")
    void interfaceIsNotConnectedWhileBranchOperationIsEmpty() {
        DiscoveredOperation matched = discoveredOperation("/api/v1/product/{cmdb}");
        DiscoveredOperation unmatched = discoveredOperation("/api/v1/other");
        unmatched.setId(10);
        unmatched.setConnectionOperationId(77);
        DiscoveredInterface discoveredInterface =
                givenArchOperationComparison(DiscoveredConnectionBranch.DESIGN, matched);
        discoveredInterface.setOperations(List.of(matched, unmatched));

        service.processOperationComparison(77);

        assertThat(matched.getConnectionOperationDesignId()).isEqualTo(77);
        assertThat(unmatched.getConnectionOperationDesignId()).isNull();
        assertThat(discoveredInterface.getConnectionInterfaceDesignId()).isNull();
    }

    @Test
    @DisplayName("CREATE/UPDATE операции не из main, develop или design не сопоставляется с discovered")
    void nonMainOperationIsNotCompared() {
        when(productBranchService.findConnectionBranchByOperationId(anyInt())).thenReturn(Optional.empty());

        service.processOperationComparison(30);

        verifyNoInteractions(operationRepository, interfaceRepository, containerRepository,
                discoveredInterfaceRepository, discoveredOperationRepository);
    }
}

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
    @DisplayName("Удаление контейнера main сбрасывает связи")
    void mainContainerDeleteClearsRelations() {
        when(productBranchService.isContainerInDefaultBranch(10)).thenReturn(true);

        service.processContainerDelete(10);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByEntityId(10);
        verify(discoveredOperationRepository).clearConnectionOperationIdByEntityId(10);
    }

    @Test
    @DisplayName("Удаление контейнера не из main связи не трогает")
    void nonMainContainerDeleteKeepsRelations() {
        when(productBranchService.isContainerInDefaultBranch(anyInt())).thenReturn(false);

        service.processContainerDelete(10);

        verifyNoInteractions(discoveredInterfaceRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("Удаление интерфейса main сбрасывает связи")
    void mainInterfaceDeleteClearsRelations() {
        when(productBranchService.isInterfaceInDefaultBranch(20)).thenReturn(true);

        service.processInterfaceDelete(20);

        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdByInterfaceId(20);
        verify(discoveredOperationRepository).clearConnectionOperationIdByInterfaceId(20);
    }

    @Test
    @DisplayName("Удаление интерфейса не из main связи не трогает")
    void nonMainInterfaceDeleteKeepsRelations() {
        when(productBranchService.isInterfaceInDefaultBranch(anyInt())).thenReturn(false);

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

    @Test
    @DisplayName("Разные имена path-параметров сопоставляются общим правилом")
    void matchesDiscoveredOperationWithADifferentParameterName() {
        Operation archOperation = new Operation();
        archOperation.setId(77);
        archOperation.setName("/api/v1/product/{code}");
        archOperation.setType("GET");
        archOperation.setInterfaceId(5);

        DiscoveredOperation discoveredOperation = new DiscoveredOperation();
        discoveredOperation.setId(9);
        discoveredOperation.setName("/api/v1/product/{cmdb}");
        discoveredOperation.setType("get");

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

        when(productBranchService.isOperationInDefaultBranch(anyInt())).thenReturn(true);
        when(operationRepository.findById(77)).thenReturn(Optional.of(archOperation));
        when(interfaceRepository.findById(5)).thenReturn(Optional.of(archInterface));
        when(containerRepository.findById(4)).thenReturn(Optional.of(container));
        when(discoveredInterfaceRepository.findAllByProductIdAndArchInterfaceIdAndConnectionInterfaceIdIsNull(41, 5))
                .thenReturn(List.of(discoveredInterface));
        when(operationRepository.findAllByIdIn(List.of(77))).thenReturn(List.of(archOperation));

        service.processOperationComparison(77);

        assertThat(discoveredOperation.getConnectionOperationId()).isEqualTo(77);
    }

    @Test
    @DisplayName("CREATE/UPDATE операции не из main не сопоставляется с discovered")
    void nonMainOperationIsNotCompared() {
        when(productBranchService.isOperationInDefaultBranch(anyInt())).thenReturn(false);

        service.processOperationComparison(30);

        verifyNoInteractions(operationRepository, interfaceRepository, containerRepository,
                discoveredInterfaceRepository, discoveredOperationRepository);
    }
}

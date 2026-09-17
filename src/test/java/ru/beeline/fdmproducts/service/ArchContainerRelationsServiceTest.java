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
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

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
    @DisplayName("CREATE/UPDATE операции не из main не сопоставляется с discovered")
    void nonMainOperationIsNotCompared() {
        when(productBranchService.isOperationInDefaultBranch(anyInt())).thenReturn(false);

        service.processOperationComparison(30);

        verifyNoInteractions(operationRepository, interfaceRepository, containerRepository,
                discoveredInterfaceRepository, discoveredOperationRepository);
    }
}

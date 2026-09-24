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
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.dto.ConnectionRequestDTO;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterfaceServiceTest {

    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private ProductBranchService productBranchService;

    @InjectMocks
    private InterfaceService service;

    private DiscoveredInterface givenDiscoveredInterface() {
        DiscoveredInterface discovered = new DiscoveredInterface();
        discovered.setId(1);
        when(discoveredInterfaceRepository.findById(1)).thenReturn(Optional.of(discovered));
        when(interfaceRepository.findById(2)).thenReturn(Optional.of(new Interface()));
        return discovered;
    }

    @Test
    @DisplayName("Интерфейс ветки main — связь проставляется")
    void connectsToMainInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        when(productBranchService.isInterfaceInDefaultBranch(2)).thenReturn(true);

        service.handConnection(new ConnectionRequestDTO(1, 2));

        assertThat(discovered.getConnectionInterfaceId()).isEqualTo(2);
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdExcept(2, 1);
    }

    @Test
    @DisplayName("Интерфейс другой ветки — 400, связь не меняется")
    void rejectsNonMainInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        when(productBranchService.isInterfaceInDefaultBranch(anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.handConnection(new ConnectionRequestDTO(1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Сопоставление возможно только с интерфейсом ветки main");

        assertThat(discovered.getConnectionInterfaceId()).isNull();
        verify(discoveredInterfaceRepository, never()).clearConnectionInterfaceIdExcept(any(), any());
        verifyNoInteractions(discoveredOperationRepository);
    }

    @Test
    @DisplayName("Снятие связи ветку не проверяет")
    void disconnectDoesNotCheckBranch() {
        givenDiscoveredInterface();

        service.handConnection(new ConnectionRequestDTO(1, null));

        verify(discoveredOperationRepository).clearConnectionOperationIdByDiscoveredInterfaceId(1);
        verifyNoInteractions(productBranchService);
    }
}

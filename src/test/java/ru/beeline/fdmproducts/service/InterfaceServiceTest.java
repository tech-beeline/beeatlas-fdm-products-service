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
import ru.beeline.fdmproducts.domain.DiscoveredConnectionBranch;
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
    @DisplayName("Интерфейс ветки main — пишется connectionInterfaceId, связи develop и design не меняются")
    void connectsToMainInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        discovered.setConnectionInterfaceDevId(7);
        discovered.setConnectionInterfaceDesignId(8);
        when(productBranchService.findConnectionBranchByInterfaceId(2))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.MAIN));

        service.handConnection(new ConnectionRequestDTO(1, 2));

        assertThat(discovered.getConnectionInterfaceId()).isEqualTo(2);
        assertThat(discovered.getConnectionInterfaceDevId()).isEqualTo(7);
        assertThat(discovered.getConnectionInterfaceDesignId()).isEqualTo(8);
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdExcept(2, 1, "main");
        verify(discoveredOperationRepository, never()).clearConnectionOperationIdByDiscoveredInterfaceId(any());
    }

    @Test
    @DisplayName("Интерфейс ветки develop — пишется connectionInterfaceDevId, у остальных снимается только develop")
    void connectsToDevelopInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        discovered.setConnectionInterfaceId(6);
        when(productBranchService.findConnectionBranchByInterfaceId(2))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DEVELOP));

        service.handConnection(new ConnectionRequestDTO(1, 2));

        assertThat(discovered.getConnectionInterfaceDevId()).isEqualTo(2);
        assertThat(discovered.getConnectionInterfaceId()).isEqualTo(6);
        assertThat(discovered.getConnectionInterfaceDesignId()).isNull();
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdExcept(2, 1, "develop");
    }

    @Test
    @DisplayName("Интерфейс ветки design — пишется connectionInterfaceDesignId")
    void connectsToDesignInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        when(productBranchService.findConnectionBranchByInterfaceId(2))
                .thenReturn(Optional.of(DiscoveredConnectionBranch.DESIGN));

        service.handConnection(new ConnectionRequestDTO(1, 2));

        assertThat(discovered.getConnectionInterfaceDesignId()).isEqualTo(2);
        assertThat(discovered.getConnectionInterfaceId()).isNull();
        assertThat(discovered.getConnectionInterfaceDevId()).isNull();
        verify(discoveredInterfaceRepository).clearConnectionInterfaceIdExcept(2, 1, "design");
    }

    @Test
    @DisplayName("Интерфейс другой ветки — 400, связь не меняется")
    void rejectsNonMainInterface() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        when(productBranchService.findConnectionBranchByInterfaceId(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handConnection(new ConnectionRequestDTO(1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Сопоставление возможно только с интерфейсом веток main, develop или design");

        assertThat(discovered.getConnectionInterfaceId()).isNull();
        verify(discoveredInterfaceRepository, never()).clearConnectionInterfaceIdExcept(any(), any(), any());
        verifyNoInteractions(discoveredOperationRepository);
    }

    @Test
    @DisplayName("Снятие связи ветку не проверяет и очищает атрибуты всех трёх веток")
    void disconnectDoesNotCheckBranch() {
        DiscoveredInterface discovered = givenDiscoveredInterface();
        discovered.setConnectionInterfaceId(2);
        discovered.setConnectionInterfaceDevId(3);
        discovered.setConnectionInterfaceDesignId(4);

        service.handConnection(new ConnectionRequestDTO(1, null));

        assertThat(discovered.getConnectionInterfaceId()).isNull();
        assertThat(discovered.getConnectionInterfaceDevId()).isNull();
        assertThat(discovered.getConnectionInterfaceDesignId()).isNull();
        verify(discoveredOperationRepository).clearConnectionOperationIdByDiscoveredInterfaceId(1);
        verify(discoveredInterfaceRepository, never()).clearConnectionInterfaceIdExcept(any(), any(), any());
        verifyNoInteractions(productBranchService);
    }
}

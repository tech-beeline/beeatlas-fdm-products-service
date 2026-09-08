/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.e2e.E2eInfoDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eProductDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2InterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationRelationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2UpsertRequestDTO;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Юнит-тесты query-параметра source у POST /api/v2/e2e (SFDM-4091). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class E2eV2ServiceTest {

    private static final String CMDB = "my-product";
    private static final Integer PRODUCT_ID = 1;
    private static final Integer INTERFACE_ID = 500;
    private static final Integer E2E_ID = 77;
    private static final String E2E_UID = "e2e-uid-1";
    private static final String INTERFACE_CODE = "iface-1";

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

    @InjectMocks
    private E2eV2Service service;

    /** Раздаёт создаваемым операциям предсказуемые id: 901, 902, ... — в порядке сохранения. */
    private final AtomicInteger operationIdSequence = new AtomicInteger(900);

    @BeforeEach
    void setUp() {
        when(productRepository.findByAliasCaseInsensitive(anyString()))
                .thenReturn(Product.builder().id(PRODUCT_ID).alias(CMDB).build());
        when(discoveredInterfaceRepository.findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(
                anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(discoveredInterfaceRepository.findAllBySourceIgnoreCaseAndExternalIdIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of());
        when(discoveredInterfaceRepository.save(any(DiscoveredInterface.class))).thenAnswer(invocation -> {
            DiscoveredInterface saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(INTERFACE_ID);
            }
            return saved;
        });
        when(discoveredOperationRepository.findByInterfaceIdAndNameAndTypeAllIgnoreCase(anyInt(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(discoveredOperationRepository.save(any(DiscoveredOperation.class))).thenAnswer(invocation -> {
            DiscoveredOperation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(operationIdSequence.incrementAndGet());
            }
            return saved;
        });
        when(e2eRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(e2eRepository.save(any(E2e.class))).thenAnswer(invocation -> {
            E2e saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(E2E_ID);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("source не передан — интерфейс создаётся с SPARX, как до SFDM-4091")
    void createsInterfaceWithSparxWhenSourceIsAbsent() {
        E2eUpsertResponseDTO response = service.upsert(request(), null);

        assertThat(response.getId()).isEqualTo(E2E_ID);
        assertThat(response.getCode()).isEqualTo(E2E_UID);
        verify(discoveredInterfaceRepository)
                .findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(eq("SPARX"), eq(PRODUCT_ID), eq(INTERFACE_CODE));
        assertThat(savedInterface().getSource()).isEqualTo("SPARX");
    }

    @Test
    @DisplayName("?source=SPARX даёт тот же результат, что и вызов без параметра")
    void explicitSparxMatchesDefault() {
        service.upsert(request(), "SPARX");

        verify(discoveredInterfaceRepository)
                .findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(eq("SPARX"), eq(PRODUCT_ID), eq(INTERFACE_CODE));
        assertThat(savedInterface().getSource()).isEqualTo("SPARX");
    }

    @Test
    @DisplayName("?source=MAPIC — интерфейс с source=MAPIC, операция привязана к нему")
    void createsInterfaceWithRequestedSource() {
        service.upsert(request(), "MAPIC");

        verify(discoveredInterfaceRepository)
                .findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(eq("MAPIC"), eq(PRODUCT_ID), eq(INTERFACE_CODE));
        DiscoveredInterface iface = savedInterface();
        assertThat(iface.getSource()).isEqualTo("MAPIC");
        assertThat(iface.getExternalId()).isEqualTo(INTERFACE_CODE);

        ArgumentCaptor<DiscoveredOperation> captor = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository).save(captor.capture());
        assertThat(captor.getValue().getDiscoveredInterface()).isSameAs(iface);
    }

    @Test
    @DisplayName("source пишется как передан, без нормализации регистра")
    void keepsSourceLetterCaseAsSent() {
        service.upsert(request(), "Structurizr");

        assertThat(savedInterface().getSource()).isEqualTo("Structurizr");
    }

    @Test
    @DisplayName("source обрезается по краям: ' MAPIC ' → 'MAPIC'")
    void trimsSource() {
        service.upsert(request(), "  MAPIC  ");

        verify(discoveredInterfaceRepository)
                .findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(eq("MAPIC"), eq(PRODUCT_ID), eq(INTERFACE_CODE));
        assertThat(savedInterface().getSource()).isEqualTo("MAPIC");
    }

    @Test
    @DisplayName("интерфейс с тем же code и продуктом, но другим source, не перезаписывается")
    void doesNotTouchInterfaceOfAnotherSource() {
        DiscoveredInterface sparxInterface = DiscoveredInterface.builder()
                .id(INTERFACE_ID)
                .externalId(INTERFACE_CODE)
                .name("старое имя")
                .source("SPARX")
                .createdDate(LocalDateTime.now())
                .build();
        when(discoveredInterfaceRepository.findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(
                eq("SPARX"), anyInt(), anyString())).thenReturn(Optional.of(sparxInterface));

        service.upsert(request(), "MAPIC");

        DiscoveredInterface created = savedInterface();
        assertThat(created).isNotSameAs(sparxInterface);
        assertThat(created.getSource()).isEqualTo("MAPIC");
        assertThat(sparxInterface.getName()).isEqualTo("старое имя");
    }

    @Test
    @DisplayName("существующий интерфейс того же source обновляется, source не меняется")
    void keepsSourceOfExistingInterface() {
        DiscoveredInterface existing = DiscoveredInterface.builder()
                .id(INTERFACE_ID)
                .externalId(INTERFACE_CODE)
                .name("старое имя")
                .source("mapic")
                .createdDate(LocalDateTime.now())
                .build();
        when(discoveredInterfaceRepository.findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(
                eq("MAPIC"), anyInt(), anyString())).thenReturn(Optional.of(existing));

        service.upsert(request(), "MAPIC");

        assertThat(existing.getName()).isEqualTo("Интерфейс 1");
        assertThat(existing.getSource()).isEqualTo("mapic");
    }

    @Test
    @DisplayName("fallback-резолв интерфейса в БД фильтрует по source из запроса")
    void fallbackLookupUsesRequestSource() {
        DiscoveredInterface fromDb = DiscoveredInterface.builder()
                .id(INTERFACE_ID)
                .externalId(INTERFACE_CODE)
                .source("MAPIC")
                .build();
        when(discoveredInterfaceRepository.findAllBySourceIgnoreCaseAndExternalIdIgnoreCase(eq("MAPIC"), eq(INTERFACE_CODE)))
                .thenReturn(List.of(fromDb));

        E2eV2UpsertRequestDTO request = request();
        request.setInterfaces(List.of());

        service.upsert(request, "MAPIC");

        verify(discoveredInterfaceRepository)
                .findAllBySourceIgnoreCaseAndExternalIdIgnoreCase(eq("MAPIC"), eq(INTERFACE_CODE));
        verify(discoveredInterfaceRepository, never()).save(any(DiscoveredInterface.class));
    }

    @Test
    @DisplayName("интерфейс есть только под другим source — 400, а не молчаливая привязка")
    void failsWhenFallbackFindsNothingForRequestSource() {
        E2eV2UpsertRequestDTO request = request();
        request.setInterfaces(List.of());

        assertThatThrownBy(() -> service.upsert(request, "MAPIC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Не найден parentInterfaceCode");
    }

    @Test
    @DisplayName("?source= и ?source=%20 — 400 с явным сообщением")
    void rejectsBlankSource() {
        assertThatThrownBy(() -> service.upsert(request(), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр source не может быть пустым");
        assertThatThrownBy(() -> service.upsert(request(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр source не может быть пустым");
        verify(e2eRepository, never()).save(any(E2e.class));
    }

    private DiscoveredInterface savedInterface() {
        ArgumentCaptor<DiscoveredInterface> captor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(captor.capture());
        return captor.getValue();
    }

    private E2eV2UpsertRequestDTO request() {
        return E2eV2UpsertRequestDTO.builder()
                .e2e(E2eInfoDTO.builder().uid(E2E_UID).name("E2E 1").build())
                .products(List.of(E2eProductDTO.builder().cmdb(CMDB).name("Продукт").build()))
                .interfaces(List.of(E2eV2InterfaceDTO.builder()
                        .code(INTERFACE_CODE)
                        .name("Интерфейс 1")
                        .parentProductCmdb(CMDB)
                        .build()))
                .operations(List.of(E2eV2OperationDTO.builder()
                        .uid("op-1")
                        .name("getSomething")
                        .type("REST")
                        .parentInterfaceCode(INTERFACE_CODE)
                        .build()))
                .operationsRelations(List.of(E2eV2OperationRelationDTO.builder()
                        .relatedOperationId("op-1")
                        .order(1)
                        .build()))
                .build();
    }
}

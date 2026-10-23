/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.E2eBranch;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.e2e.E2eInfoDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eProductDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2InterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationRelationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2PatchRequestDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2UpsertRequestDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eBranchRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SFDM-4244: продукт-владелец e2e в POST и PATCH /api/v2/e2e, связи на discovered_operation. */
class E2eV2OwnerTest {

    private static final String CMDB = "my-product";
    private static final String OWNER_ALIAS = "CHECKOUT";
    private static final Integer OWNER_ID = 7;
    private static final Integer E2E_ID = 77;
    private static final String E2E_UID = "e2e-uid-1";

    private ProductRepository productRepository;
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private DiscoveredOperationRepository discoveredOperationRepository;
    private E2eRepository e2eRepository;
    private OperationRelationRepository operationRelationRepository;
    private OperationRepository operationRepository;
    private E2eV2Service service;

    private final AtomicInteger discoveredIds = new AtomicInteger(900);
    private E2e stored;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        discoveredInterfaceRepository = mock(DiscoveredInterfaceRepository.class);
        discoveredOperationRepository = mock(DiscoveredOperationRepository.class);
        e2eRepository = mock(E2eRepository.class);
        operationRelationRepository = mock(OperationRelationRepository.class);
        operationRepository = mock(OperationRepository.class);
        E2eBranchRepository e2eBranchRepository = mock(E2eBranchRepository.class);
        service = new E2eV2Service(productRepository, discoveredInterfaceRepository, discoveredOperationRepository,
                e2eRepository, operationRelationRepository, operationRepository,
                new E2eDiscoveredOperationCatalog(operationRepository, mock(SlaRepository.class),
                        mock(InterfaceRepository.class), mock(ContainerRepository.class)),
                new E2eOwnerService(productRepository),
                new E2eBranchService(e2eBranchRepository, operationRelationRepository));
        when(e2eBranchRepository.upsert(anyString(), anyString())).thenReturn(E2E_ID);

        when(productRepository.findByAliasCaseInsensitive(anyString()))
                .thenReturn(Product.builder().id(1).alias(CMDB).build());
        when(productRepository.findFirstByAliasIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(productRepository.findFirstByAliasIgnoreCase(OWNER_ALIAS.toLowerCase()))
                .thenReturn(Optional.of(Product.builder().id(OWNER_ID).alias(OWNER_ALIAS).build()));
        when(productRepository.findFirstByAliasIgnoreCase(OWNER_ALIAS))
                .thenReturn(Optional.of(Product.builder().id(OWNER_ID).alias(OWNER_ALIAS).build()));
        when(discoveredInterfaceRepository.findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(
                anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(discoveredInterfaceRepository.save(any(DiscoveredInterface.class))).thenAnswer(invocation -> {
            DiscoveredInterface saved = invocation.getArgument(0);
            saved.setId(500);
            return saved;
        });
        when(discoveredOperationRepository.findAllByInterfaceIdIn(any())).thenReturn(List.of());
        when(discoveredOperationRepository.save(any(DiscoveredOperation.class))).thenAnswer(invocation -> {
            DiscoveredOperation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(discoveredIds.incrementAndGet());
            }
            return saved;
        });
        when(operationRepository.findAllByIdInAndDeletedDateIsNull(any()))
                .thenReturn(List.of(Operation.builder().id(100).build()));
        when(e2eRepository.findByCode(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(e2eRepository.findByCodeIgnoreCase(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(e2eRepository.save(any(E2e.class))).thenAnswer(invocation -> {
            E2e saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(E2E_ID);
            }
            stored = saved;
            return saved;
        });
    }

    @Test
    @DisplayName("Создание без владельца — product_id остаётся null")
    void createsWithoutOwner() {
        service.upsert(request(null), "STRUCTURIZR", null);

        assertThat(stored.getProductId()).isNull();
        assertThat(stored.getSource()).isEqualTo("STRUCTURIZR");
    }

    @Test
    @DisplayName("Создание с владельцем: alias ищется после trim без учёта регистра")
    void createsWithOwner() {
        service.upsert(request("  " + OWNER_ALIAS + " "), null, null);

        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("Неизвестный alias — 404, ни продукт, ни e2e, ни discovered-методы не создаются")
    void unknownOwnerIsNotFoundAndNothingIsWritten() {
        assertThatThrownBy(() -> service.upsert(request("UNKNOWN"), null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт с указанным alias не найден");
        verify(productRepository, never()).save(any(Product.class));
        verify(e2eRepository, never()).save(any(E2e.class));
        verify(discoveredOperationRepository, never()).save(any(DiscoveredOperation.class));
    }

    @Test
    @DisplayName("productAlias из пробелов — 400")
    void blankOwnerIsBadRequest() {
        assertThatThrownBy(() -> service.upsert(request("   "), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productAlias не может быть пустым");
        verify(e2eRepository, never()).save(any(E2e.class));
    }

    @Test
    @DisplayName("Повторный POST без поля владельца его сохраняет, пустой productAlias снимает")
    void repeatedPostKeepsOwnerAndEmptyStringClearsIt() {
        service.upsert(request(OWNER_ALIAS), null, null);
        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);

        service.upsert(request(null), null, null);
        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);

        service.upsert(request(""), null, null);
        assertThat(stored.getProductId()).isNull();
    }

    @Test
    @DisplayName("Повторный POST без name не затирает название")
    void repeatedPostKeepsName() {
        service.upsert(request(OWNER_ALIAS), null, null);
        E2eV2UpsertRequestDTO withoutName = request(null);
        withoutName.getE2e().setName(null);

        service.upsert(withoutName, null, null);

        assertThat(stored.getName()).isEqualTo("E2E 1");
    }

    @Test
    @DisplayName("PATCH: null не меняет владельца, пустая строка снимает, неизвестный alias — 404")
    void patchHandlesOwner() {
        service.upsert(request(OWNER_ALIAS), null, null);

        service.patch(E2E_UID, E2eV2PatchRequestDTO.builder().name("Новое").build(), null);
        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);
        assertThat(stored.getName()).isEqualTo("Новое");

        assertThatThrownBy(() -> service.patch(E2E_UID, E2eV2PatchRequestDTO.builder().productAlias("UNKNOWN").build(), null))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);

        service.patch(E2E_UID, E2eV2PatchRequestDTO.builder().productAlias("").build(), null);
        assertThat(stored.getProductId()).isNull();

        service.patch(E2E_UID, E2eV2PatchRequestDTO.builder().productAlias(OWNER_ALIAS).build(), null);
        assertThat(stored.getProductId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("Связь пишется на id discovered_operation, сопоставленная операция — в connection_operation_id")
    void writesRelationsOnDiscoveredOperations() {
        E2eV2UpsertRequestDTO request = request(null);
        request.setOperations(List.of(
                operation("op-1", "/cart", null),
                operation("op-2", "/pay", 100)));
        request.setOperationsRelations(List.of(
                E2eV2OperationRelationDTO.builder().relatedOperationId("op-1").order(1).build(),
                E2eV2OperationRelationDTO.builder().operationId("op-1").relatedOperationId("op-2").order(1).build()));

        service.upsert(request, null, null);

        ArgumentCaptor<DiscoveredOperation> discovered = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository, org.mockito.Mockito.times(2)).save(discovered.capture());
        assertThat(discovered.getAllValues()).extracting(DiscoveredOperation::getId, DiscoveredOperation::getConnectionOperationId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(901, null),
                        org.assertj.core.groups.Tuple.tuple(902, 100));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OperationRelation>> relations = ArgumentCaptor.forClass(List.class);
        verify(operationRelationRepository).saveAll(relations.capture());
        assertThat(relations.getValue()).extracting(OperationRelation::getOperationId, OperationRelation::getRelatedOperationId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(null, 901),
                        org.assertj.core.groups.Tuple.tuple(901, 902));
    }

    private static E2eV2OperationDTO operation(String uid, String name, Integer connectionOperationId) {
        return E2eV2OperationDTO.builder()
                .uid(uid)
                .name(name)
                .type("GET")
                .parentInterfaceCode("iface-1")
                .connectionOperationId(connectionOperationId)
                .build();
    }

    private E2eV2UpsertRequestDTO request(String productAlias) {
        return E2eV2UpsertRequestDTO.builder()
                .e2e(E2eInfoDTO.builder().uid(E2E_UID).name("E2E 1").productAlias(productAlias).build())
                .products(List.of(E2eProductDTO.builder().cmdb(CMDB).name("Продукт").build()))
                .interfaces(List.of(E2eV2InterfaceDTO.builder()
                        .code("iface-1")
                        .name("Интерфейс 1")
                        .parentProductCmdb(CMDB)
                        .build()))
                .operations(List.of(operation("op-1", "getSomething", null)))
                .operationsRelations(List.of(E2eV2OperationRelationDTO.builder()
                        .relatedOperationId("op-1")
                        .order(1)
                        .build()))
                .build();
    }
}

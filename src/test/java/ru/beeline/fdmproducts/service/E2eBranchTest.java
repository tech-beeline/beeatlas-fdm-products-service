/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.E2eBranch;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.e2e.E2eInfoDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eProductDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2GetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2InterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationRelationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2PatchRequestDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2RelationTreeNodeDTO;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SFDM-4250: ветка e2e в POST, PATCH и GET /api/v2/e2e. Ветки и связи хранятся в памяти по правилам таблиц
 * e2e_branch и operation_relations: upsert ветки по (code, branch_name), связи по e2e_branch_id.
 */
class E2eBranchTest {

    private static final String CMDB = "my-product";
    private static final Integer E2E_ID = 7;
    private static final String CODE = "E2E-ORDER";

    private E2eBranchRepository e2eBranchRepository;
    private OperationRelationRepository operationRelationRepository;
    private E2eRepository e2eRepository;
    private E2eV2Service service;

    private final List<E2eBranch> branches = new ArrayList<>();
    private final List<OperationRelation> relations = new ArrayList<>();
    private final AtomicInteger branchIds = new AtomicInteger(E2E_ID - 1);
    private final AtomicInteger relationIds = new AtomicInteger();
    private final AtomicInteger discoveredIds = new AtomicInteger(900);
    private E2e stored;

    @BeforeEach
    void setUp() {
        ProductRepository productRepository = mock(ProductRepository.class);
        DiscoveredInterfaceRepository discoveredInterfaceRepository = mock(DiscoveredInterfaceRepository.class);
        DiscoveredOperationRepository discoveredOperationRepository = mock(DiscoveredOperationRepository.class);
        OperationRepository operationRepository = mock(OperationRepository.class);
        e2eRepository = mock(E2eRepository.class);
        e2eBranchRepository = mock(E2eBranchRepository.class);
        operationRelationRepository = mock(OperationRelationRepository.class);
        service = new E2eV2Service(productRepository, discoveredInterfaceRepository, discoveredOperationRepository,
                e2eRepository, operationRelationRepository, operationRepository,
                new E2eDiscoveredOperationCatalog(operationRepository, mock(SlaRepository.class),
                        mock(InterfaceRepository.class), mock(ContainerRepository.class)),
                new E2eOwnerService(productRepository),
                new E2eBranchService(e2eBranchRepository, operationRelationRepository));

        when(productRepository.findByAliasCaseInsensitive(anyString()))
                .thenReturn(Product.builder().id(1).alias(CMDB).build());
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
        when(e2eRepository.findByCode(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(e2eRepository.findByCodeIgnoreCase(anyString())).thenAnswer(invocation -> Optional.ofNullable(stored)
                .filter(e2e -> e2e.getCode().equalsIgnoreCase(invocation.getArgument(0))));
        when(e2eRepository.save(any(E2e.class))).thenAnswer(invocation -> {
            E2e saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(E2E_ID);
            }
            stored = saved;
            return saved;
        });

        when(e2eBranchRepository.findAllByCodeAndBranchNameIgnoreCaseOrderByIdAsc(anyString(), anyString()))
                .thenAnswer(invocation -> branches.stream()
                        .filter(branch -> branch.getCode().equals(invocation.getArgument(0))
                                && branch.getBranchName().equalsIgnoreCase(invocation.getArgument(1)))
                        .toList());
        when(e2eBranchRepository.upsert(anyString(), anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            String branchName = invocation.getArgument(1);
            return branches.stream()
                    .filter(branch -> branch.getCode().equals(code) && branch.getBranchName().equals(branchName))
                    .findFirst()
                    .orElseGet(() -> {
                        E2eBranch created = E2eBranch.builder().id(branchIds.incrementAndGet()).code(code)
                                .branchName(branchName).build();
                        branches.add(created);
                        return created;
                    })
                    .getId();
        });

        when(operationRelationRepository.saveAll(any())).thenAnswer(invocation -> {
            Collection<OperationRelation> saved = invocation.getArgument(0);
            saved.forEach(relation -> relation.setId(relationIds.incrementAndGet()));
            relations.addAll(saved);
            return new ArrayList<>(saved);
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            Integer branchId = invocation.getArgument(0);
            relations.removeIf(relation -> relation.getE2eBranchId().equals(branchId));
            return null;
        }).when(operationRelationRepository).deleteAllByE2eBranchId(any());
        when(operationRelationRepository.findAllByE2eBranchId(any())).thenAnswer(invocation -> relations.stream()
                .filter(relation -> relation.getE2eBranchId().equals(invocation.getArgument(0)))
                .toList());
    }

    @Test
    @DisplayName("POST без branch создаёт карточку и ветку main, дерево пишется на её e2e_branch_id")
    void postWithoutBranchCreatesMain() {
        E2eUpsertResponseDTO response = service.upsert(request("op-1"), null, null);

        assertThat(response.getId()).isEqualTo(E2E_ID);
        assertThat(response.getCode()).isEqualTo(CODE);
        assertThat(response.getBranch()).isEqualTo("main");
        assertThat(branches).singleElement().satisfies(branch -> {
            assertThat(branch.getCode()).isEqualTo(CODE);
            assertThat(branch.getBranchName()).isEqualTo("main");
            assertThat(response.getE2eBranchId()).isEqualTo(branch.getId());
        });
        assertThat(relations).extracting(OperationRelation::getE2eBranchId).containsOnly(response.getE2eBranchId());
    }

    @Test
    @DisplayName("Повторный POST в ту же ветку обновляет карточку и заново пишет дерево только этой ветки")
    void repeatedPostRewritesSameBranch() {
        E2eUpsertResponseDTO first = service.upsert(request("op-1"), null, null);
        E2eV2UpsertRequestDTO second = request("op-1");
        second.getE2e().setName("Новое имя");

        E2eUpsertResponseDTO response = service.upsert(second, null, "main");

        assertThat(response.getE2eBranchId()).isEqualTo(first.getE2eBranchId());
        assertThat(branches).hasSize(1);
        assertThat(relations).hasSize(1);
        assertThat(stored.getName()).isEqualTo("Новое имя");
    }

    @Test
    @DisplayName("POST в design той же карточки: вторая строка e2e_branch, дерево main остаётся")
    void secondBranchKeepsFirstTree() {
        E2eUpsertResponseDTO main = service.upsert(request("op-1"), null, null);
        List<OperationRelation> mainTree = List.copyOf(relations);

        E2eUpsertResponseDTO design = service.upsert(request("op-2"), null, "design");

        assertThat(design.getId()).isEqualTo(main.getId());
        assertThat(design.getBranch()).isEqualTo("design");
        assertThat(design.getE2eBranchId()).isNotEqualTo(main.getE2eBranchId());
        assertThat(branches).extracting(E2eBranch::getBranchName).containsExactly("main", "design");
        assertThat(relations).containsAll(mainTree);
        assertThat(relations).filteredOn(relation -> relation.getE2eBranchId().equals(design.getE2eBranchId()))
                .hasSize(1);
        // Связи main удалялись только при первой записи в main.
        verify(operationRelationRepository, times(1)).deleteAllByE2eBranchId(main.getE2eBranchId());
    }

    @Test
    @DisplayName("Main и main — одна ветка: вторая строка не создаётся, в таблице branch_name = main")
    void mainInAnotherCaseIsTheSameBranch() {
        E2eUpsertResponseDTO main = service.upsert(request("op-1"), null, null);

        E2eUpsertResponseDTO upper = service.upsert(request("op-2"), null, " Main ");

        assertThat(upper.getE2eBranchId()).isEqualTo(main.getE2eBranchId());
        assertThat(upper.getBranch()).isEqualTo("main");
        assertThat(branches).extracting(E2eBranch::getBranchName).containsExactly("main");
        verify(e2eBranchRepository).upsert(CODE, "main");
    }

    @Test
    @DisplayName("Новая ветка пишется после trim в нижнем регистре")
    void newBranchIsStoredLowerCase() {
        E2eUpsertResponseDTO response = service.upsert(request("op-1"), null, "  Design ");

        assertThat(response.getBranch()).isEqualTo("design");
        verify(e2eBranchRepository).upsert(CODE, "design");
    }

    @Test
    @DisplayName("Пустой branch — 400 в POST, PATCH и GET, база не пишется и ветка не создаётся")
    void blankBranchIsRejected() {
        assertThatThrownBy(() -> service.upsert(request("op-1"), null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр branch не может быть пустым");
        assertThatThrownBy(() -> service.patch(CODE, E2eV2PatchRequestDTO.builder().build(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр branch не может быть пустым");
        assertThatThrownBy(() -> service.getByCode(CODE, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр branch не может быть пустым");

        verify(e2eRepository, never()).save(any());
        verify(e2eBranchRepository, never()).upsert(anyString(), anyString());
    }

    @Test
    @DisplayName("PATCH без operationsRelations обновляет карточку, связи веток не трогает, ветка в ответе")
    void patchWithoutTreeKeepsRelations() {
        E2eUpsertResponseDTO main = service.upsert(request("op-1"), null, null);
        service.upsert(request("op-2"), null, "design");
        List<OperationRelation> before = List.copyOf(relations);

        E2eUpsertResponseDTO response = service.patch(CODE, E2eV2PatchRequestDTO.builder().name("Новое").build(), null);

        assertThat(stored.getName()).isEqualTo("Новое");
        assertThat(relations).containsExactlyElementsOf(before);
        assertThat(response.getE2eBranchId()).isEqualTo(main.getE2eBranchId());
        assertThat(response.getBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("PATCH с деревом заменяет связи только указанной ветки; пустой массив оставляет её пустой")
    void patchWithTreeReplacesOnlyThatBranch() {
        E2eUpsertResponseDTO main = service.upsert(request("op-1"), null, null);
        E2eUpsertResponseDTO design = service.upsert(request("op-2"), null, "design");
        List<OperationRelation> mainTree = relations.stream()
                .filter(relation -> relation.getE2eBranchId().equals(main.getE2eBranchId()))
                .toList();

        service.patch(CODE, E2eV2PatchRequestDTO.builder().operationsRelations(List.of()).build(), "design");

        assertThat(mainTree).isNotEmpty();
        assertThat(relations).containsExactlyElementsOf(mainTree);
        assertThat(service.getByCode(CODE, "design").getOperationsRelations()).isEmpty();
        assertThat(branches).extracting(E2eBranch::getId).contains(design.getE2eBranchId());
    }

    @Test
    @DisplayName("PATCH неизвестного code — 404, ветка не создаётся")
    void patchOfUnknownCodeIsNotFound() {
        assertThatThrownBy(() -> service.patch("NO-SUCH", E2eV2PatchRequestDTO.builder().build(), "design"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("E2e с указанным кодом не найден");

        verify(e2eBranchRepository, never()).upsert(anyString(), anyString());
        assertThat(branches).isEmpty();
    }

    @Test
    @DisplayName("GET v2 без branch читает дерево main, с branch=design — дерево design")
    void getReadsRequestedBranch() {
        service.upsert(request("op-1"), null, null);
        service.upsert(request("op-2", "op-3"), null, "design");

        E2eV2GetResponseDTO main = service.getByCode(CODE, null);
        E2eV2GetResponseDTO design = service.getByCode(CODE, "DESIGN");

        assertThat(main.getOperationsRelations()).extracting(E2eV2RelationTreeNodeDTO::getRelatedOperationId)
                .containsExactly(901);
        assertThat(design.getOperationsRelations()).extracting(E2eV2RelationTreeNodeDTO::getRelatedOperationId)
                .containsExactly(902, 903);
    }

    @Test
    @DisplayName("GET v2 неизвестной ветки — 200 и пустое дерево, строка ветки не создаётся")
    void getOfUnknownBranchIsEmpty() {
        service.upsert(request("op-1"), null, null);

        E2eV2GetResponseDTO response = service.getByCode(CODE, "release");

        assertThat(response.getE2e().getCode()).isEqualTo(CODE);
        assertThat(response.getOperationsRelations()).isEmpty();
        assertThat(branches).extracting(E2eBranch::getBranchName).containsExactly("main");
        verify(e2eBranchRepository, never()).upsert(CODE, "release");
    }

    @Test
    @DisplayName("Строки, различающиеся регистром, — берётся равная нормализованному имени, иначе с меньшим id")
    void picksNormalizedBranchAmongCaseDuplicates() {
        E2eBranchService branchService = new E2eBranchService(e2eBranchRepository, operationRelationRepository);
        branches.add(E2eBranch.builder().id(3).code(CODE).branchName("Design").build());
        branches.add(E2eBranch.builder().id(5).code(CODE).branchName("design").build());
        branches.add(E2eBranch.builder().id(8).code(CODE).branchName("Release").build());
        branches.add(E2eBranch.builder().id(9).code(CODE).branchName("RELEASE").build());

        assertThat(branchService.find(CODE, "design")).get().extracting(E2eBranch::getId).isEqualTo(5);
        assertThat(branchService.find(CODE, "release")).get().extracting(E2eBranch::getId).isEqualTo(8);
    }

    private static E2eV2OperationDTO operation(String uid) {
        return E2eV2OperationDTO.builder()
                .uid(uid)
                .name("/" + uid.toLowerCase(Locale.ROOT))
                .type("GET")
                .parentInterfaceCode("iface-1")
                .build();
    }

    /** Корневые связи на каждую операцию в порядке перечисления. */
    private E2eV2UpsertRequestDTO request(String... operationUids) {
        List<E2eV2OperationDTO> operations = new ArrayList<>();
        List<E2eV2OperationRelationDTO> roots = new ArrayList<>();
        for (int i = 0; i < operationUids.length; i++) {
            operations.add(operation(operationUids[i]));
            roots.add(E2eV2OperationRelationDTO.builder().relatedOperationId(operationUids[i]).order(i + 1).build());
        }
        return E2eV2UpsertRequestDTO.builder()
                .e2e(E2eInfoDTO.builder().uid(CODE).name("E2E").build())
                .products(List.of(E2eProductDTO.builder().cmdb(CMDB).name("Продукт").build()))
                .interfaces(List.of(E2eV2InterfaceDTO.builder()
                        .code("iface-1")
                        .name("Интерфейс 1")
                        .parentProductCmdb(CMDB)
                        .build()))
                .operations(operations)
                .operationsRelations(roots)
                .build();
    }
}

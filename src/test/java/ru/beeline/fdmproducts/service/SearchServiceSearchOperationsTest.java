/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.OperationSearchDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.mapper.ArchOperationMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** GET /api/v1/operation — фильтр по alias / ветке main. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceSearchOperationsTest {

    private static final String PATH = "/api/v1/orders";
    private static final String ALIAS = "orders-product";
    private static final int BRANCH_ID = 42;

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;

    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductBranchService productBranchService;

    @Spy
    private ArchOperationMapper archOperationMapper = new ArchOperationMapper();

    @InjectMocks
    private SearchService searchService;

    private Product product() {
        return Product.builder().id(1).name("Заказы").alias(ALIAS).build();
    }

    private ProductBranch mainBranch() {
        return ProductBranch.builder().id(BRANCH_ID).alias(ALIAS).branchName("main").build();
    }

    private ArchOperationProjection projection(int opId) {
        ArchOperationProjection projection = mock(ArchOperationProjection.class);
        when(projection.getOpId()).thenReturn(opId);
        when(projection.getOpName()).thenReturn(PATH);
        when(projection.getOpType()).thenReturn("GET");
        when(projection.getInterfaceId()).thenReturn(101);
        when(projection.getInterfaceName()).thenReturn("orders");
        when(projection.getInterfaceCode()).thenReturn("orders-api");
        when(projection.getContainerId()).thenReturn(11);
        when(projection.getContainerName()).thenReturn("orders-svc");
        when(projection.getContainerCode()).thenReturn("orders-svc");
        when(projection.getProductId()).thenReturn(1);
        when(projection.getProductName()).thenReturn("Заказы");
        when(projection.getProductAlias()).thenReturn(ALIAS);
        when(projection.getProductBranchName()).thenReturn("main");
        return projection;
    }

    @Test
    @DisplayName("Пустой path — 400")
    void emptyPathRejected() {
        assertThatThrownBy(() -> searchService.searchOperations("", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр path не должен быть пустым.");
        verifyNoInteractions(productRepository, productBranchService);
    }

    @Test
    @DisplayName("?alias= и пробелы — 400")
    void blankAliasRejected() {
        assertThatThrownBy(() -> searchService.searchOperations(PATH, null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр alias не может быть пустым");
        assertThatThrownBy(() -> searchService.searchOperations(PATH, null, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр alias не может быть пустым");
        verifyNoInteractions(productRepository, productBranchService, operationRepository);
    }

    @Test
    @DisplayName("Продукт не найден — 404")
    void unknownProductNotFound() {
        when(productRepository.findByAliasCaseInsensitive("unknown")).thenReturn(null);

        assertThatThrownBy(() -> searchService.searchOperations(PATH, null, "unknown"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт не найден.");
        verifyNoInteractions(productBranchService, operationRepository);
    }

    @Test
    @DisplayName("alias с пробелами и другим регистром — trim + поиск продукта без учёта регистра")
    void trimsAliasAndFindsProductIgnoreCase() {
        List<ArchOperationProjection> matches = List.of(projection(5));
        when(productRepository.findByAliasCaseInsensitive("Orders-Product")).thenReturn(product());
        when(productBranchService.find(ALIAS, null)).thenReturn(Optional.of(mainBranch()));
        when(operationRepository.findArchOperationsProjectionByBranch(PATH, BRANCH_ID)).thenReturn(matches);

        OperationSearchDTO result = searchService.searchOperations(PATH, null, "  Orders-Product  ");

        assertThat(result.getArchOperations()).hasSize(1);
        assertThat(result.getDiscoveredOperations()).isEmpty();
        verify(productRepository).findByAliasCaseInsensitive("Orders-Product");
        verify(productBranchService).find(ALIAS, null);
    }

    @Test
    @DisplayName("Продукт без ветки main — 200 и пустые массивы")
    void missingMainBranchReturnsEmpty() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(product());
        when(productBranchService.find(ALIAS, null)).thenReturn(Optional.empty());

        OperationSearchDTO result = searchService.searchOperations(PATH, null, ALIAS);

        assertThat(result.getArchOperations()).isEmpty();
        assertThat(result.getDiscoveredOperations()).isEmpty();
        verifyNoInteractions(operationRepository, discoveredOperationRepository);
    }

    @Test
    @DisplayName("С alias — поиск только по id ветки из find(alias, null), discovered не добирается")
    void withAliasFiltersByBranchAndSkipsDiscovered() {
        List<ArchOperationProjection> matches = List.of(projection(5));
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(product());
        when(productBranchService.find(ALIAS, null)).thenReturn(Optional.of(mainBranch()));
        when(operationRepository.findArchOperationsProjectionByBranch(PATH, BRANCH_ID)).thenReturn(matches);

        OperationSearchDTO result = searchService.searchOperations(PATH, null, ALIAS);

        assertThat(result.getArchOperations()).hasSize(1);
        assertThat(result.getArchOperations().get(0).getId()).isEqualTo(5);
        assertThat(result.getDiscoveredOperations()).isEmpty();
        verify(operationRepository).findArchOperationsProjectionByBranch(PATH, BRANCH_ID);
        verify(operationRepository, never()).findArchOperationsProjection(anyString());
        verifyNoInteractions(discoveredOperationRepository);
    }

    @Test
    @DisplayName("С alias и type — тот же фильтр ветки")
    void withAliasAndTypeUsesBranchQuery() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(product());
        when(productBranchService.find(ALIAS, null)).thenReturn(Optional.of(mainBranch()));
        when(operationRepository.findArchOperationsProjectionByTypeAndBranch(PATH, "GET", BRANCH_ID))
                .thenReturn(List.of());

        OperationSearchDTO result = searchService.searchOperations(PATH, "GET", ALIAS);

        assertThat(result.getArchOperations()).isEmpty();
        assertThat(result.getDiscoveredOperations()).isEmpty();
        verify(operationRepository).findArchOperationsProjectionByTypeAndBranch(PATH, "GET", BRANCH_ID);
        verifyNoInteractions(discoveredOperationRepository);
    }

    @Test
    @DisplayName("Без alias — прежний поиск и добор discovered")
    void withoutAliasKeepsDiscoveredFill() {
        when(operationRepository.findArchOperationsProjection(PATH)).thenReturn(List.of());
        when(discoveredOperationRepository.findAllByNameAndDeletedDateIsNull(eq(PATH), eq(50)))
                .thenReturn(List.of());

        OperationSearchDTO result = searchService.searchOperations(PATH, null, null);

        assertThat(result.getArchOperations()).isEmpty();
        assertThat(result.getDiscoveredOperations()).isEmpty();
        verify(operationRepository).findArchOperationsProjection(PATH);
        verify(discoveredOperationRepository).findAllByNameAndDeletedDateIsNull(PATH, 50);
        verify(productRepository, never()).findByAliasCaseInsensitive(any());
        verifyNoInteractions(productBranchService);
        verify(operationRepository, never()).findArchOperationsProjectionByBranch(anyString(), anyInt());
    }
}

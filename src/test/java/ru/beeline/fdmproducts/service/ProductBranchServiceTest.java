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
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SFDM-4043: разбор query-параметра branch и поиск ветки по замечаниям тестирования. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductBranchServiceTest {

    private static final String ALIAS = "orders-product";

    @Mock
    private ProductBranchRepository productBranchRepository;

    @InjectMocks
    private ProductBranchService service;

    private ProductBranch branch(int id, String name) {
        return ProductBranch.builder().id(id).alias(ALIAS).branchName(name).build();
    }

    @Test
    @DisplayName("Параметр не передан — main")
    void absentBranchIsMain() {
        assertThat(service.resolveBranchName(null)).isEqualTo("main");
    }

    @Test
    @DisplayName("?branch= и ?branch=%20 — 400, а не молчаливый main")
    void blankBranchRejected() {
        assertThatThrownBy(() -> service.resolveBranchName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр branch не может быть пустым");
        assertThatThrownBy(() -> service.resolveBranchName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр branch не может быть пустым");
    }

    @Test
    @DisplayName("Имя обрезается по краям и приводится к нижнему регистру")
    void branchNameNormalized() {
        assertThat(service.resolveBranchName("  Design  ")).isEqualTo("design");
    }

    @Test
    @DisplayName("Поиск не зависит от регистра записи в БД")
    void findsBranchStoredInAnotherCase() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "design"))
                .thenReturn(List.of(branch(7, "Design")));

        assertThat(service.find(ALIAS, "DESIGN")).map(ProductBranch::getId).contains(7);
    }

    @Test
    @DisplayName("Ветки, различающиеся только регистром: выигрывает совпадающая с нормализованным именем")
    void prefersExactMatchAmongCaseDuplicates() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of(branch(3, "Main"), branch(9, "main")));

        assertThat(service.find(ALIAS, null)).map(ProductBranch::getId).contains(9);
    }

    @Test
    @DisplayName("Если точного совпадения нет — самая ранняя ветка, одна и та же для всех методов")
    void fallsBackToEarliestAmongCaseDuplicates() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of(branch(3, "Main"), branch(9, "MAIN")));

        assertThat(service.find(ALIAS, null)).map(ProductBranch::getId).contains(3);
    }

    @Test
    @DisplayName("Ветки нет — пусто; для GET это 200 с пустым результатом")
    void missingBranchIsEmpty() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(anyString(), anyString()))
                .thenReturn(List.of());

        assertThat(service.find(ALIAS, "develop")).isEmpty();
    }

    @Test
    @DisplayName("Существующая ветка в другом регистре не порождает вторую: ON CONFLICT её не поймает")
    void doesNotDuplicateBranchDifferingByCase() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of(branch(7, "Main")));

        assertThat(service.getOrCreateId(ALIAS, "MAIN")).isEqualTo(7);
        verify(productBranchRepository, never()).upsert(any(), any());
    }

    @Test
    @DisplayName("Ветки нет — создаётся под нормализованным именем")
    void createsMissingBranch() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "design"))
                .thenReturn(List.of());
        when(productBranchRepository.upsert(ALIAS, "design")).thenReturn(42);

        assertThat(service.getOrCreateId(ALIAS, " Design ")).isEqualTo(42);
    }

    @Test
    @DisplayName("Пустой branch не создаёт ветку")
    void blankBranchCreatesNothing() {
        assertThatThrownBy(() -> service.getOrCreateId(ALIAS, " "))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productBranchRepository, never()).upsert(any(), any());
    }

    @Test
    @DisplayName("SFDM-4098: назначение без ветки создаёт отсутствующую main")
    void assignmentCreatesMissingMain() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of());
        when(productBranchRepository.upsert(ALIAS, "main")).thenReturn(5);

        assertThat(service.getExistingOrMainId(ALIAS, null)).isEqualTo(5);
    }

    @Test
    @DisplayName("SFDM-4098: назначение на существующую ветку не создаёт новую")
    void assignmentUsesExistingBranch() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "design"))
                .thenReturn(List.of(branch(8, "design")));

        assertThat(service.getExistingOrMainId(ALIAS, "Design")).isEqualTo(8);
        verify(productBranchRepository, never()).upsert(any(), any());
    }

    @Test
    @DisplayName("SFDM-4098: назначение на отсутствующую ветку, отличную от main, — 404")
    void assignmentToMissingBranchRejected() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "design"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getExistingOrMainId(ALIAS, "design"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Ветка design продукта не найдена");
        verify(productBranchRepository, never()).upsert(any(), any());
    }

    @Test
    @DisplayName("SFDM-4098: принадлежность arch-объекта ветке main — без учёта регистра")
    void archObjectBranchCheck() {
        when(productBranchRepository.findBranchNameByContainerId(1)).thenReturn(Optional.of("Main"));
        when(productBranchRepository.findBranchNameByInterfaceId(2)).thenReturn(Optional.of("design"));
        when(productBranchRepository.findBranchNameByOperationId(3)).thenReturn(Optional.empty());

        assertThat(service.isContainerInDefaultBranch(1)).isTrue();
        assertThat(service.isInterfaceInDefaultBranch(2)).isFalse();
        assertThat(service.isOperationInDefaultBranch(3)).isFalse();
    }
}

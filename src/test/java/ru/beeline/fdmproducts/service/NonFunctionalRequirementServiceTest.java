/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirement;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.nfr.RequirementProductDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonFunctionalRequirementServiceTest {

    private static final String ALIAS = "orders-product";
    private static final int PRODUCT_ID = 1;

    @Mock
    private NonFunctionalRequirementRepository nonFunctionalRequirementRepository;
    @Mock
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductBranchRepository productBranchRepository;
    @Mock
    private ProductBranchService productBranchService;

    @InjectMocks
    private NonFunctionalRequirementService service;

    private final ProductBranch main = ProductBranch.builder().id(7).alias(ALIAS).branchName("main").build();
    private final ProductBranch design = ProductBranch.builder().id(8).alias(ALIAS).branchName("design").build();

    @BeforeEach
    void setUp() {
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(Product.builder().id(PRODUCT_ID).alias(ALIAS).build()));
    }

    @Test
    @DisplayName("POST без ветки назначает требования на main")
    void addWithoutBranchAssignsToMain() {
        when(productBranchService.getExistingOrMainId(ALIAS, null)).thenReturn(7);
        when(productBranchRepository.findById(7)).thenReturn(Optional.of(main));
        NonFunctionalRequirementEnum nfr = NonFunctionalRequirementEnum.builder().id(100).build();
        when(nonFunctionalRequirementEnumRepository.findAllById(List.of(100))).thenReturn(List.of(nfr));
        when(nonFunctionalRequirementRepository.findByProductBranchIdAndNfrIds(7, List.of(100))).thenReturn(List.of());

        service.addProductNfr(PRODUCT_ID, null, null, null, null, List.of(100));

        verify(nonFunctionalRequirementRepository).save(argThat(
                saved -> saved.getProductBranch() == main && saved.getNfr() == nfr && "Beeatlas".equals(saved.getSource())));
    }

    @Test
    @DisplayName("POST на отсутствующую ветку — 404, назначений нет")
    void addToMissingBranchRejected() {
        when(productBranchService.getExistingOrMainId(ALIAS, "design"))
                .thenThrow(new EntityNotFoundException("Ветка design продукта не найдена"));

        assertThatThrownBy(() -> service.addProductNfr(PRODUCT_ID, null, null, "design", null, List.of(100)))
                .isInstanceOf(EntityNotFoundException.class);
        verify(nonFunctionalRequirementRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET по отсутствующей ветке — пустой список")
    void getForMissingBranchIsEmpty() {
        when(productBranchService.findId(ALIAS, "design")).thenReturn(Optional.empty());

        assertThat(service.getProductNfr(PRODUCT_ID, null, null, "design")).isEmpty();
        verify(nonFunctionalRequirementRepository, never()).findByProductBranchIdWithNfrAndCore(anyInt());
    }

    @Test
    @DisplayName("DELETE снимает требование только с указанной ветки")
    void deleteUsesBranch() {
        when(productBranchService.findId(ALIAS, "design")).thenReturn(Optional.of(8));
        NonFunctionalRequirement rel = NonFunctionalRequirement.builder().id(50).productBranch(design).source("Иванов").build();
        when(nonFunctionalRequirementRepository.findByProductBranch_IdAndNfr_Id(8, 100)).thenReturn(Optional.of(rel));

        service.deleteProductNfr(100, PRODUCT_ID, null, null, "design");

        verify(nonFunctionalRequirementRepository).delete(rel);
    }

    @Test
    @DisplayName("Удаление авто-связей: связь другой ветки того же продукта отклоняется")
    void deleteRelationsOfAnotherBranchRejected() {
        when(productBranchService.findId(ALIAS, null)).thenReturn(Optional.of(7));
        NonFunctionalRequirement rel = NonFunctionalRequirement.builder().id(50).productBranch(design).source("Beeatlas").build();
        when(nonFunctionalRequirementRepository.findAllByIdInWithProductBranch(List.of(50))).thenReturn(List.of(rel));

        assertThatThrownBy(() -> service.deleteBeeatlasProductNfrRelations(PRODUCT_ID, null, null, null, List.of(50)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Связь 50 не принадлежит указанному продукту");
        verify(nonFunctionalRequirementRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Продукты по требованию: без ветки — только назначения на main, ветка в ответе")
    void productsByRequirementFilteredByBranch() {
        when(productBranchService.resolveBranchName(null)).thenReturn("main");
        when(nonFunctionalRequirementEnumRepository.existsById(100)).thenReturn(true);
        when(nonFunctionalRequirementRepository.findByNfrIdWithProductBranch(100)).thenReturn(List.of(
                NonFunctionalRequirement.builder().productBranch(main).source("Beeatlas").build(),
                NonFunctionalRequirement.builder().productBranch(design).source("Beeatlas").build()));

        List<RequirementProductDTO> products = service.getProductsByRequirementId("100", null, null);

        assertThat(products).containsExactly(new RequirementProductDTO(ALIAS, "main", "Beeatlas"));
    }
}

/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.ProductPutDto;
import ru.beeline.fdmproducts.dto.PutUpdateProductDTO;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.UserProductRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceCreateMainBranchTest {

    private static final String ALIAS = "orders-product";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductBranchRepository productBranchRepository;
    @Mock
    private UserProductRepository userProductRepository;

    @InjectMocks
    private ProductService service;

    @Test
    @DisplayName("PUT /product/{code}: создание продукта создаёт ветку main")
    void createOrUpdateCreatesMainBranch() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(null);

        service.createOrUpdate(ProductPutDto.builder().name("Заказы").build(), ALIAS);

        ArgumentCaptor<ProductBranch> branchCaptor = ArgumentCaptor.forClass(ProductBranch.class);
        verify(productBranchRepository).save(branchCaptor.capture());
        assertThat(branchCaptor.getValue().getAlias()).isEqualTo(ALIAS);
        assertThat(branchCaptor.getValue().getBranchName()).isEqualTo("main");
    }

    @Test
    @DisplayName("PUT /product/{code}: обновление не создаёт ветку")
    void createOrUpdateDoesNotCreateBranchOnUpdate() {
        Product existing = Product.builder().id(1).alias(ALIAS).name("Старое").build();
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(existing);

        service.createOrUpdate(ProductPutDto.builder().name("Новое").build(), ALIAS);

        verify(productBranchRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /product: создание продукта создаёт ветку main")
    void updateProductCreatesMainBranch() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(null);

        service.updateProduct(PutUpdateProductDTO.builder()
                .alias(ALIAS)
                .name("Заказы")
                .employeesIds(List.of())
                .build());

        ArgumentCaptor<ProductBranch> branchCaptor = ArgumentCaptor.forClass(ProductBranch.class);
        verify(productBranchRepository).save(branchCaptor.capture());
        assertThat(branchCaptor.getValue().getAlias()).isEqualTo(ALIAS);
        assertThat(branchCaptor.getValue().getBranchName()).isEqualTo("main");
    }
}

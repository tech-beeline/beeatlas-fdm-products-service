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
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.mapper.ArchOperationMapper;
import ru.beeline.fdmproducts.repository.OperationRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceGetOperationByTcTest {

    private static final int TC_ID = 51420;

    @Mock
    private OperationRepository operationRepository;

    @Spy
    private ArchOperationMapper archOperationMapper = new ArchOperationMapper();

    @InjectMocks
    private SearchService searchService;

    @Test
    @DisplayName("Одна операция без product_branch не роняет список остальных")
    void skipsOperationWithoutProductBranch() {
        Operation healthy = operation(5, "healthy", branch("main"));
        Operation broken = operation(6529, "C.Pay", null);
        broken.getInterfaceObj().getContainerProduct().setProductBranchId(null);

        when(operationRepository.findOperationsWithFullChainGraph(TC_ID))
                .thenReturn(List.of(healthy, broken));

        List<ArchOperationDTO> result = searchService.getOperationByTc(TC_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5);
        assertThat(result.get(0).getProduct().getBranchName()).isEqualTo("main");
    }

    @Test
    @DisplayName("Все операции без ветки — пустой список, без 404")
    void allBrokenReturnsEmptyList() {
        Operation broken = operation(6529, "C.Pay", null);
        broken.getInterfaceObj().getContainerProduct().setProductBranchId(null);

        when(operationRepository.findOperationsWithFullChainGraph(TC_ID))
                .thenReturn(List.of(broken));

        assertThat(searchService.getOperationByTc(TC_ID)).isEmpty();
    }

    private Operation operation(int id, String name, ProductBranch branch) {
        Product product = Product.builder().id(1).name("Заказы").alias("orders").build();
        if (branch != null) {
            branch.setProduct(product);
        }
        ContainerProduct container = ContainerProduct.builder()
                .id(11)
                .name("orders-backend")
                .code("obe")
                .productBranchId(branch == null ? null : branch.getId())
                .productBranch(branch)
                .build();
        Interface iface = Interface.builder()
                .id(101)
                .name("Orders API")
                .code("orders-api")
                .containerId(11)
                .containerProduct(container)
                .build();
        return Operation.builder()
                .id(id)
                .name(name)
                .type("GET")
                .tcId(TC_ID)
                .interfaceId(101)
                .interfaceObj(iface)
                .build();
    }

    private ProductBranch branch(String branchName) {
        return ProductBranch.builder().id(7).alias("orders").branchName(branchName).build();
    }
}

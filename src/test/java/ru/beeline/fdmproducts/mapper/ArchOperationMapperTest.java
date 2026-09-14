/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchOperationMapperTest {

    private final ArchOperationMapper mapper = new ArchOperationMapper();

    @Test
    @DisplayName("mapToArchOperationDTO(Operation) заполняет branchName из product_branch")
    void mapsBranchNameFromProductBranch() {
        Operation operation = operationWithBranch("feature/orders");

        ArchOperationDTO dto = mapper.mapToArchOperationDTO(operation);

        assertThat(dto.getProduct().getId()).isEqualTo(1);
        assertThat(dto.getProduct().getAlias()).isEqualTo("orders");
        assertThat(dto.getProduct().getBranchName()).isEqualTo("feature/orders");
    }

    @Test
    @DisplayName("Нет product_branch по product_branch_id — 404")
    void missingProductBranchThrows404() {
        Operation operation = operationWithBranch("main");
        operation.getInterfaceObj().getContainerProduct().setProductBranch(null);
        operation.getInterfaceObj().getContainerProduct().setProductBranchId(42);

        assertThatThrownBy(() -> mapper.mapToArchOperationDTO(operation))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ветка продукта не найдена")
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("Нет product по alias ветки — 404")
    void missingProductThrows404() {
        Operation operation = operationWithBranch("main");
        operation.getInterfaceObj().getContainerProduct().getProductBranch().setProduct(null);

        assertThatThrownBy(() -> mapper.mapToArchOperationDTO(operation))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт с alias 'orders' не найден");
    }

    private Operation operationWithBranch(String branchName) {
        Product product = Product.builder().id(1).name("Заказы").alias("orders").build();
        ProductBranch branch = ProductBranch.builder()
                .id(7)
                .alias("orders")
                .branchName(branchName)
                .product(product)
                .build();
        ContainerProduct container = ContainerProduct.builder()
                .id(11)
                .name("orders-backend")
                .code("obe")
                .productBranchId(7)
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
                .id(5)
                .name("/api/v1/orders")
                .type("GET")
                .interfaceId(101)
                .interfaceObj(iface)
                .build();
    }
}

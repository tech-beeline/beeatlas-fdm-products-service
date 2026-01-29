/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ContainerDTO;

import java.util.Date;

@Component
public class ContainerMapper {
    public ContainerProduct convertToContainerProduct(ContainerDTO containerDTO, Product product) {
        return ContainerProduct.builder()
                .productId(product.getId())
                .name(containerDTO.getName())
                .code(containerDTO.getCode())
                .version(containerDTO.getVersion())
                .createdDate(new Date())
                .build();
    }

    public void updateContainerProduct(ContainerProduct container, ContainerDTO containerDTO, Product product) {
        container.setProductId(product.getId());
        container.setName(containerDTO.getName());
        container.setVersion(containerDTO.getVersion());
        container.setUpdatedDate(new Date());
    }
}

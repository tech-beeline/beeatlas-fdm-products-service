package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.dto.ContainerSearchDTO;
import ru.beeline.fdmproducts.dto.InterfaceSearchDTO;
import ru.beeline.fdmproducts.dto.ProductSearchDTO;
import ru.beeline.fdmproducts.dto.search.MatchedArchOperationDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;

@Component
public class ArchOperationMapper {

    public ArchOperationDTO mapToArchOperationDTO(ArchOperationProjection proj) {
        return ArchOperationDTO.builder()
                .id(proj.getOpId())
                .name(proj.getOpName())
                .type(proj.getOpType())
                .interfaceObj(InterfaceSearchDTO.builder()
                        .id(proj.getInterfaceId())
                        .name(proj.getInterfaceName())
                        .code(proj.getInterfaceCode())
                        .build())
                .container(ContainerSearchDTO.builder()
                        .id(proj.getContainerId())
                        .name(proj.getContainerName())
                        .code(proj.getContainerCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(proj.getProductId())
                        .name(proj.getProductName())
                        .alias(proj.getProductAlias())
                        .branchName(proj.getProductBranchName())
                        .build())
                .build();
    }

    public MatchedArchOperationDTO mapToMatchedArchOperationDTO(ArchOperationProjection proj, String productCode) {
        return MatchedArchOperationDTO.builder()
                .id(proj.getOpId())
                .name(proj.getOpName())
                .type(proj.getOpType())
                .productCode(productCode)
                .interfaceObj(InterfaceSearchDTO.builder()
                        .id(proj.getInterfaceId())
                        .name(proj.getInterfaceName())
                        .code(proj.getInterfaceCode())
                        .build())
                .container(ContainerSearchDTO.builder()
                        .id(proj.getContainerId())
                        .name(proj.getContainerName())
                        .code(proj.getContainerCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(proj.getProductId())
                        .name(proj.getProductName())
                        .alias(proj.getProductAlias())
                        .branchName(proj.getProductBranchName())
                        .build())
                .build();
    }

    public ArchOperationDTO mapToArchOperationDTO(Operation operation) {
        ContainerProduct container = operation.getInterfaceObj().getContainerProduct();
        ProductBranch productBranch = resolveProductBranch(container);
        Product product = resolveProduct(productBranch);

        return ArchOperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .type(operation.getType())
                .interfaceObj(InterfaceSearchDTO.builder()
                        .id(operation.getInterfaceId())
                        .name(operation.getInterfaceObj().getName())
                        .code(operation.getInterfaceObj().getCode())
                        .build())
                .container(ContainerSearchDTO.builder()
                        .id(operation.getInterfaceObj().getContainerId())
                        .name(container.getName())
                        .code(container.getCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .alias(product.getAlias())
                        .branchName(productBranch.getBranchName())
                        .build())
                .build();
    }

    private ProductBranch resolveProductBranch(ContainerProduct container) {
        ProductBranch productBranch = container.getProductBranch();
        if (productBranch == null) {
            throw new EntityNotFoundException(
                    "Ветка продукта не найдена: product_branch_id=" + container.getProductBranchId());
        }
        return productBranch;
    }

    private Product resolveProduct(ProductBranch productBranch) {
        Product product = productBranch.getProduct();
        if (product == null) {
            throw new EntityNotFoundException(
                    "Продукт с alias '" + productBranch.getAlias() + "' не найден");
        }
        return product;
    }
}

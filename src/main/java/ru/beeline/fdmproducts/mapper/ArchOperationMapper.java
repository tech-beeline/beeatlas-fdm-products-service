package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Operation;

import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.dto.ContainerSearchDTO;
import ru.beeline.fdmproducts.dto.InterfaceSearchDTO;
import ru.beeline.fdmproducts.dto.ProductSearchDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;

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
                        .build())
                .build();
    }

    public ArchOperationDTO mapToArchOperationDTO(Operation operation) {
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
                        .name(operation.getInterfaceObj().getContainerProduct().getName())
                        .code(operation.getInterfaceObj().getContainerProduct().getCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(operation.getInterfaceObj().getContainerProduct().getProductId())
                        .name(operation.getInterfaceObj().getContainerProduct().getProduct().getName())
                        .alias(operation.getInterfaceObj().getContainerProduct().getProduct().getAlias())
                        .build())
                .build();
    }
}

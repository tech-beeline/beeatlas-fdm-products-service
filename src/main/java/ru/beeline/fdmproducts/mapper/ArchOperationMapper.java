package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.dto.search.ArchOperationDTO;
import ru.beeline.fdmproducts.dto.search.ContainerSearchDTO;
import ru.beeline.fdmproducts.dto.search.InterfaceSearchDTO;
import ru.beeline.fdmproducts.dto.search.ProductSearchDTO;
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
}

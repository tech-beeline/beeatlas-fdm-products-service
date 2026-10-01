/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredInterfaceByIdsDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.dto.discovered.OperationWithTechCapabilityDTO;

@Component
public class DiscoveredOperationByIdsMapper {

    public DiscoveredOperationByIdsDTO toDto(DiscoveredOperation discoveredOperation,
                                             DiscoveredInterface discoveredInterface,
                                             Operation operation,
                                             TcDTO techCapability) {
        return DiscoveredOperationByIdsDTO.builder()
                .id(discoveredOperation.getId())
                .interfaceId(discoveredOperation.getInterfaceId())
                .connectionOperationId(discoveredOperation.getConnectionOperationId())
                .name(discoveredOperation.getName())
                .context(discoveredOperation.getContext())
                .description(discoveredOperation.getDescription())
                .tcCode(discoveredOperation.getTcCode())
                .tcDescription(discoveredOperation.getTcDescription())
                .isPlanned(discoveredOperation.getIsPlanned())
                .type(discoveredOperation.getType())
                .returnType(discoveredOperation.getReturnType())
                .rps(discoveredOperation.getRps())
                .latency(discoveredOperation.getLatency())
                .errorRate(discoveredOperation.getErrorRate())
                .createdDate(discoveredOperation.getCreatedDate())
                .updatedDate(discoveredOperation.getUpdatedDate())
                .deletedDate(discoveredOperation.getDeletedDate())
                .discoveredInterface(toInterfaceDto(discoveredInterface))
                .operation(toOperationDto(operation, techCapability))
                .build();
    }

    private DiscoveredInterfaceByIdsDTO toInterfaceDto(DiscoveredInterface discoveredInterface) {
        if (discoveredInterface == null) {
            return null;
        }
        Integer productId = discoveredInterface.getProduct() == null
                ? null
                : discoveredInterface.getProduct().getId();
        return DiscoveredInterfaceByIdsDTO.builder()
                .id(discoveredInterface.getId())
                .name(discoveredInterface.getName())
                .externalId(discoveredInterface.getExternalId())
                .apiId(discoveredInterface.getApiId())
                .connectionInterfaceId(discoveredInterface.getConnectionInterfaceId())
                .apiLink(discoveredInterface.getApiLink())
                .version(discoveredInterface.getVersion())
                .description(discoveredInterface.getDescription())
                .status(discoveredInterface.getStatus())
                .context(discoveredInterface.getContext())
                .source(discoveredInterface.getSource())
                .productId(productId)
                .productPlanned(discoveredInterface.getProductPlanned())
                .createdDate(discoveredInterface.getCreatedDate())
                .updatedDate(discoveredInterface.getUpdatedDate())
                .deletedDate(discoveredInterface.getDeletedDate())
                .build();
    }

    private OperationWithTechCapabilityDTO toOperationDto(Operation operation, TcDTO techCapability) {
        if (operation == null) {
            return null;
        }
        return OperationWithTechCapabilityDTO.builder()
                .id(operation.getId())
                .interfaceId(operation.getInterfaceId())
                .name(operation.getName())
                .description(operation.getDescription())
                .type(operation.getType())
                .returnType(operation.getReturnType())
                .tcId(operation.getTcId())
                .isDeletedTc(operation.getIsDeletedTc())
                .createdDate(operation.getCreatedDate())
                .updatedDate(operation.getUpdatedDate())
                .deletedDate(operation.getDeletedDate())
                .techCapability(techCapability)
                .build();
    }
}

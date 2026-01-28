/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmlib.dto.product.*;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.dto.MapicOperationFullDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DiscoveredOperationMapper {

    @Autowired
    DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public List<MapicOperationFullDTO> createMapicOperationFullDTO(List<DiscoveredOperation> discoveredOperations) {
        List<MapicOperationFullDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation discoveredOperation : discoveredOperations) {
                {
                    String contextApi = null;
                    Optional<DiscoveredInterface> optDiscoveredInterface = discoveredInterfaceRepository.findById(
                            discoveredOperation.getInterfaceId());
                    if (optDiscoveredInterface.isPresent()) {
                        contextApi = optDiscoveredInterface.get().getContext();
                    }
                    result.add(MapicOperationFullDTO.builder()
                            .id(discoveredOperation.getId())
                            .name(discoveredOperation.getName())
                            .type(discoveredOperation.getType())
                            .description(discoveredOperation.getDescription())
                            .context(discoveredOperation.getContext())
                            .contextApi(contextApi)
                            .build());
                }
            }
        }
        return result;
    }

    public DiscoveredOperationDTO mapToOperationDTO(DiscoveredOperation discoveredOperation,
                                                    ArchOperationProjection projection) {
        return DiscoveredOperationDTO.builder()
                .id(discoveredOperation.getId())
                .name(discoveredOperation.getName())
                .type(discoveredOperation.getType())
                .connectionOperation(ConnectionOperationDTO.builder()
                        .id(projection.getOpId())
                        .name(projection.getOpName())
                        .type(projection.getOpType())
                        .build())
                .interfaceObj(InterfaceSearchDTO.builder()
                        .id(projection.getInterfaceId())
                        .code(projection.getInterfaceCode())
                        .name(projection.getInterfaceName())
                        .build())
                .container(ContainerSearchDTO.builder()
                        .id(projection.getContainerId())
                        .name(projection.getContainerName())
                        .code(projection.getContainerCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(projection.getProductId())
                        .name(projection.getProductName())
                        .alias(projection.getProductAlias())
                        .build())
                .build();
    }
}
